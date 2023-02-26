# Modu-Messenger Backend

## Project Info

### Backend

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Cloud](https://spring.io/projects/spring-cloud)
- [Spring Cloud Api Gateway](https://spring.io/projects/spring-cloud-gateway)
- [Spring Cloud Eureka](https://cloud.spring.io/spring-cloud-netflix/reference/html/)
- [Spring Cloud Config](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/)
- [Spring Cloud OpenFeign](https://cloud.spring.io/spring-cloud-openfeign/reference/html)
- [Spring Security](https://spring.io/projects/spring-security)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Spring Querydsl](http://querydsl.com/)
- [Spring WebSocket](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [Spring AMQP](https://spring.io/projects/spring-amqp)
- [Springfox Swagger UI](http://springfox.github.io/springfox/docs/current/)
- [JSON Web Tokens](https://jwt.io/)

### Database

- [MySQL](https://www.mysql.com/)
- [Redis](https://redis.io/)

### Infra Structure

- [Amazon Web Services](https://aws.amazon.com/)
- [Docker](https://www.docker.com/)
- [k8s](https://kubernetes.io/ko/)
- [minio](https://min.io/)
- [Rabbit MQ](https://www.rabbitmq.com/)
- [Zipkin](https://zipkin.io/)

## Project Architecture

### MSA (Micro Service Architecture)

모두의 메신저 프로젝트는 마이크로 서비스 아키텍처를 기반으로 하고 있습니다.  

## Project Structure

### Spring API Gateway
스프링 api gateway 를 통해 jwt 토큰 인증 과정을 가장 앞단에서 처리  
jwt 검증을 통해 인가된 사용자는 resource로의 접근이 가능  

### Spring Eureka

### Spring config-server

### rabbit-mq 를 이용한 메시징 처리

## Dev Diary

### 채팅 서버
채팅 기능은 메인 기능 이므로, 가장 많은 부하가 걸린다. 이를 개선하기 위해 여러 컨테이너를 띄워 문제를 해결하려고 했다.  
근데 http와 같은 비연결형 통신이 아닌 연결형 통신인 웹 소켓을 사용하고 있어 고민을 하게되었다.  
단체 채팅방의 경우, 혹은 1대1 채팅방이라고 할지라도 만약에 다른 컨테이너에 연결된 유저에게 어떻게 채팅을 전송할까 하는 문제였다.  
여러가지 방법이 있곘지만, Redis의 Pub/Sub을 통해 해결하였다.  
모든 채팅 서버는 구동할때 새로운 채팅이 있음을 전달 받을 채널을 Subsribe 한다.  
클라이언트가 채팅을 전송하면, 이를 데이터 베이스에 새로운 채팅 메시지를 저장한다.  
그리고 새로운 채팅 메세지의 정보와 채팅방 정보를 Subscribe 중인 다른 채팅 서버에게 Publish 한다.  
Subscribe 중인 채널로 부터 다른 채팅 서버에서 보낸 데이터가 전달 되면,  
데이터 베이스에서 전송 받은 채팅 방의 정보와 채팅 정보를 조회한다.  
조회한 채팅 메세지를 채팅방에 속해있는 유저들에게 전송한다. 이때 연결되어있지 않은 유저에게는 전송하지 않는다.  

### 채팅방 - 유저 (다대다 양방향 연관관계)
초기 설계는 다대다 연관관계 (@ManyToMany)를 사용하여 양방향 관계로 설계   
그러나 프로젝트 요구사항을 채팅방 - 유저간의 다대다 연관관계를 테이블 두개의 조인만으로 해결할수 없을 것으로 보임   
예를 들어 채팅방 - 유저 연관관계 사이에 데이터가 추가로 필요한 경우 두개의 테이블의 맵핑이 불가능함   
연관 테이블은 채팅방멤버(임시 이름)테이블을 두어 다대다 연관관계를 1대다의 연관관계로 풀어나감  
채팅방은 - 채팅방 멤버 - 유저 (1-N, N-1, 모두 양방향)  
채팅방, 유저 테이블은 본 프로젝트의 핵심이되는 테이블로 부하가 가장 많이 생기는 테이블   
따라서 쿼리가 추가로 발생하는 상황 (N+1 문제)를 최대한 피하고자 초기설게에서 연관관계를 수정함   

### 채팅 페이징 처리 (No offset 페이징 처리)
초기 설계는 페이징 처리를 통해서 오프셋과 사이즈를 통해서 처리를 하려고 했다.  
하지만 채팅의 경우 1페이지에서 3페이지로 건너뛰는 경우가 없다.  
스크롤하다가 스크롤이 끝에 도달한 경우에만 이전 데이터를 가져오면 된다.  
그래서 정해진 사이즈와 마지막으로 표시된 채팅의 정보만으로 조회가 가능하다.  

그런데 마지막으로 표시된 채팅이 없는 경우가 존재한다. 어떤 경우일까?  
하나는 채팅방에 처음 입장하는 순간이다. 마지막으로 표시된 채팅이 없기 때문이다.  
이경우에는 채팅방의 채팅을 조회한뒤 마지막에서부터 정해진 크기만큼의 데이터만을 조회한다.  
두번째는 채팅방을 처음 생성한 경우이다. 처음 생성하면 이전 채팅 내역이 존재하지 않는다.  
따라서 채팅방의 채팅 갯수를 가져오는 api를 통해 채팅 조회 요청을 전송할지를 결정해줘야 한다.  

### 푸쉬 알림 (파이어 베이스 클라우드 메시징)
파이버 베이스 클라우드 메시징 (fcm) 연동을 통해, 푸쉬 알림을 전송하는 기능을 추가하였음.  
전송 방식은 토큰을 기준으로 전송하는 방법과 토픽을 통해 전송하는 방식 두개가 있다.  
초기 설계는 클라이언트가 토큰을 발급 받고 백엔드로 전송하면, 백엔드에서 이를 관리한다.  
그리고 메시지가 전송될때마다 채팅방별로 저장된 유저의 토큰을 가지고 와서, 파이어 베이스로 전송하는 것이었다.  
그러나 채팅방 멤버의 수가 많아지면 많아질수록 멤버의 수만큼 loop를 돌면서 전송해야한다.  

그래서 topic을 통해 전송 하는 방법으로 설계를 변경하였다. (토픽은 채팅방 id을 가지고 사용한다)  
topic을 통해 푸시 알림을 전송하면 토큰 발급 후 백엔드로 전송할 필요가 없다.  
(전체 알림 등을 보낼때는 필요하다. 그래서 추후에 전체 알림을 위해 전송 로직은 그대로 남겨 두었다.)  
클라이너트는 구독하려는 토픽만 구독하고, 백엔드는 메시지 이벤트 발생시에 topic을 가지고 파이어 베이스로 전송한다.  
그러면 파이어 베이스에서 topic 을 구독중인 기기로 푸시 알림을 전송한다.   
topic을 이용하면 채팅방 내에 속한 멤버들에게 개별 적으로 토큰을 가지고 전송 전송할 필요가 없어진다.  

만일 전체 유저에게 푸쉬 알림을 전송해야하는 경우에는 일반 메시지 타입이 아닌 멀티 캐스트 메시지를 사용한다.  
멀티 캐스트 메시지의 경우 보낼수있는 수신자가 최대 1000명이다. 그래서 적당한 숫자로 나눠서 전송해준다.  
나는 1000개의 절반인 500개로 설정하였다. 전체 유저에게 푸시 알림을 전송하는 경우 500개 단위로 나눠서 전송한다.  

### 채팅방 - 채팅  
@OneToMany (주체는 채팅방)
1대다의 연관관계를 가짐  
채팅방은 다수의 채팅을, 채팅은 채팅방 하나를 가진다.  

### 유저 - 기타  
친구 리스트 등은 유저의 id로만 관리중임  
즐겨찾는 친구, 숨긴 친구, 차단된 친구 기능 추가를 할때마다  
엔티티에 필드를 추가해야하므로, 친구 관련된 엔티티를 따로 만들어 추가  
친구 엔티티를 상속 받는, 즐겨찾기, 차단, 숨김 친구 엔티티를 생성

### 마이크로 서비스 아키텍쳐
서비스의 단위를 쪼개서 공통된 최소한의 요청을 하는 모듈로 분리후 구동   
회원 관리 서버, 채팅 서버, 이미지 서버, 인증 서버, 캐시 서버 등등   
각각의 서버는 컨테이너로 구동되며, k8s를 통해 컨테이너를 관리   
스케일업 (aws 인스턴스 사양 upgrade), 스케일 아웃 (aws 인스턴스, k8s 파드 추가) 고려 필요   

### 공통 데이터 api
도메인 모델과는 전혀 상관없는 데이터 (버전, 시간 동기화, health-check 요청 등)를 전송하기 위한 엔티티   
엔티티는 key-value 두개의 값으로 이뤄져 있으며   
서버측 데이터 베이스에 저장되지 않는 데이터 (고정된 값 ex. 버전, 자주 갱신 또는 쉽게 생성 가능한 데이터 ex. 시간)   
데이터의 저장 주체는 메모리, Repository 에서 데이터베이스에 접근하지 않음   
서버 구동시, 혹은 필요시에 데이터를 생성후 메모리 업로드하고, 요청에 응답   

### 이미지 서버
Go로 된 이미지 서버를 구동 하려고 계획 하였으나,
스프링 부트를 통해 개발 하는것을 고민중  
멀티 모듈을 통해 다른 포트로 서버 구동 가능  

### 인증 서버
인증 토큰 발급 및 토큰 필터링은 스프링 시큐리티를 이용해서 사용중  
스프링 클라우드로 api 게이트 웨이를 이용하여, 별도의 인증서버에서 토큰 발급 및 인증  
redis를 활용한 캐시 서버를 활용해 인증시에 발생하는 부하 감소   

### 데이터 베이스
개발 환경, 테스트 환경에서는 동일한 데이터 베이스를 사용하고 있음  
운영 환경에서는 각각의 파트에 서로 다른 데이터 베이스에 접근 가능  

### 개발, 테스트, 운영 환경 분리
서버 프로파일 분리를 통해서 개발, 운영, 테스트 환경에 따라 서로 다른 프로파일로 서버를 구동   
테스트시 h2 데이터 베이스 사용, 개발 및 운영시 mysql   
테스트시에는 로컬에서 서버 구동, 운영 및 개발시에는 도커 컨테이너로 구동   