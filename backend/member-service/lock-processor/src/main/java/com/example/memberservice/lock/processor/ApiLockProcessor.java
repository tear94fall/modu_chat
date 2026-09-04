package com.example.memberservice.lock.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.HashSet;
import java.util.Set;

/**
 * {@code @ApiLock} 이 붙은 메서드에 {@code @LockParam} 파라미터가 정확히 존재하고,
 * 그 파라미터의 타입으로 락 키를 만들 수 있는지 컴파일 시점에 검증한다.
 */
@SupportedAnnotationTypes({
        ApiLockProcessor.API_LOCK_ANNOTATION,
        ApiLockProcessor.LOCK_PARAM_ANNOTATION
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class ApiLockProcessor extends AbstractProcessor {

    static final String API_LOCK_ANNOTATION = "com.example.memberservice.global.lock.ApiLock";
    static final String LOCK_PARAM_ANNOTATION = "com.example.memberservice.global.lock.LockParam";
    private static final String LOCKABLE_TYPE = "com.example.memberservice.global.lock.Lockable";
    private static final String UUID_TYPE = "java.util.UUID";
    private static final String CHAR_SEQUENCE_TYPE = "java.lang.CharSequence";
    private static final String STRING_TYPE = "java.lang.String";

    private static final Set<String> SUPPORTED_SIMPLE_TYPES = Set.of(
            STRING_TYPE,
            CHAR_SEQUENCE_TYPE,
            UUID_TYPE,
            "java.lang.Byte",
            "java.lang.Short",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Float",
            "java.lang.Double",
            "java.lang.Boolean",
            "java.lang.Character"
    );

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Messager messager = processingEnv.getMessager();
        Types typeUtils = processingEnv.getTypeUtils();
        Elements elementUtils = processingEnv.getElementUtils();

        TypeElement lockableElement = elementUtils.getTypeElement(LOCKABLE_TYPE);
        TypeMirror lockableType = lockableElement == null ? null : lockableElement.asType();

        TypeElement apiLockAnnotationElement = findAnnotation(annotations, API_LOCK_ANNOTATION);
        if (apiLockAnnotationElement != null) {
            for (Element element : roundEnv.getElementsAnnotatedWith(apiLockAnnotationElement)) {
                if (!(element instanceof ExecutableElement method)) {
                    continue;
                }

                Set<VariableElement> lockParams = findLockParamParameters(method);
                if (lockParams.isEmpty()) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "@ApiLock 을 쓰려면 파라미터 하나에 @LockParam 을 붙여야 합니다", method);
                    continue;
                }

                for (VariableElement param : lockParams) {
                    validateParamType(param, typeUtils, lockableType, messager);
                }
            }
        }

        TypeElement lockParamAnnotationElement = findAnnotation(annotations, LOCK_PARAM_ANNOTATION);
        if (lockParamAnnotationElement != null) {
            for (Element element : roundEnv.getElementsAnnotatedWith(lockParamAnnotationElement)) {
                if (!(element instanceof VariableElement param)) {
                    continue;
                }
                Element enclosingMethod = param.getEnclosingElement();
                if (!(enclosingMethod instanceof ExecutableElement method) || !hasAnnotation(method, API_LOCK_ANNOTATION)) {
                    messager.printMessage(Diagnostic.Kind.WARNING,
                            "@ApiLock 이 없는 메서드에 @LockParam 이 붙어 있습니다 (무시됩니다)", param);
                }
            }
        }

        return false;
    }

    private TypeElement findAnnotation(Set<? extends TypeElement> annotations, String qualifiedName) {
        for (TypeElement annotation : annotations) {
            if (annotation.getQualifiedName().contentEquals(qualifiedName)) {
                return annotation;
            }
        }
        return null;
    }

    private Set<VariableElement> findLockParamParameters(ExecutableElement method) {
        Set<VariableElement> result = new HashSet<>();
        for (VariableElement param : method.getParameters()) {
            if (hasAnnotation(param, LOCK_PARAM_ANNOTATION)) {
                result.add(param);
            }
        }
        return result;
    }

    private boolean hasAnnotation(Element element, String qualifiedName) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            TypeElement annotationType = (TypeElement) mirror.getAnnotationType().asElement();
            if (annotationType.getQualifiedName().contentEquals(qualifiedName)) {
                return true;
            }
        }
        return false;
    }

    private void validateParamType(VariableElement param, Types typeUtils, TypeMirror lockableType, Messager messager) {
        TypeMirror paramType = param.asType();

        if (lockableType != null && typeUtils.isAssignable(paramType, lockableType)) {
            return;
        }

        if (paramType.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) paramType;
            TypeElement typeElement = (TypeElement) declaredType.asElement();

            if (typeElement.getKind() == ElementKind.ENUM) {
                return;
            }

            String qualifiedName = typeElement.getQualifiedName().toString();
            if (SUPPORTED_SIMPLE_TYPES.contains(qualifiedName)) {
                return;
            }
        } else if (paramType instanceof PrimitiveType) {
            // 기본형은 항상 허용한다.
            return;
        }

        messager.printMessage(Diagnostic.Kind.ERROR,
                "@LockParam 은 Lockable 을 구현하거나 String·기본형·enum·UUID 여야 키를 만들 수 있습니다: "
                        + paramType,
                param);
    }
}
