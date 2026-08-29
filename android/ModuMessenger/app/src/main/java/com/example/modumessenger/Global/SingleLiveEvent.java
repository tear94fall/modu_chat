package com.example.modumessenger.Global;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 1회성 이벤트용 LiveData.
 * 일반 LiveData 는 최신값을 보존하므로, 화면 회전이나 Fragment 재부착 시
 * 지나간 배너가 다시 표시된다. pending 플래그로 한 번만 전달되게 한다.
 */
public class SingleLiveEvent<T> extends MutableLiveData<T> {

    private final AtomicBoolean pending = new AtomicBoolean(false);

    @MainThread
    @Override
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        super.observe(owner, value -> {
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(value);
            }
        });
    }

    @MainThread
    @Override
    public void setValue(@Nullable T value) {
        pending.set(true);
        super.setValue(value);
    }

    @Override
    public void postValue(@Nullable T value) {
        pending.set(true);
        super.postValue(value);
    }

    /**
     * 보류 중인 이벤트를 버린다. 로그아웃처럼 수신자가 바뀌는 경계에서 호출한다.
     * 이 호출 없이는 미소비 이벤트가 다음 옵저버에게 전달된다.
     */
    @MainThread
    public void clearPending() {
        pending.set(false);
    }
}
