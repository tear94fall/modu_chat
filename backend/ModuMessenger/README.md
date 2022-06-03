# Modu-Messenger Backend

# Backend

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Querydsl](http://querydsl.com/)
- [Spring WebSocket](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [Springfox Swagger UI](http://springfox.github.io/springfox/docs/current/)
- [JSON Web Tokens](https://jwt.io/)
- [MySQL](https://www.mysql.com/)
- [Amazon Web Services](https://aws.amazon.com/)

# 연관 관계

### 프록시
서버 가장 앞단에 nginx를 프록시로 구동  
컨테이너의의 서버들은 내부에서는 통신이 가능  
다만 외부에서의 직접 접근이 불가능 하도록 설정  

### 채팅방 - 유저
현재는 별도의 엔티티로 필요할때 요청을 통해 데이터 응답 받음  
채팅방 - 유저간의 다대다 연관관계를 테이블 두개의 조인만으로 해결할수 없을 것으로 보임  
채팅방, 유저 테이블은 본 프로젝트의 핵심이되는 테이블로 부하가 가장 많이 생기는 테이블  
연관 테이블은 채팅방멤버(임시 이름)테이블을 두어 다대다 연관관계를 1대다의 연관관계로 풀어나감  
채팅방은 - 채팅방 멤버 - 유저 (1-N, N-1)  

### 채팅방 - 채팅  
1대다의 연관관계를 가짐  
채팅방은 다수의 채팅을, 채팅은 채팅방 하나를 가진다.  

### 유저 - 기타  
친구 리스트 등은 유저의 id로만 관리중임  
즐겨찾는 친구, 숨긴 친구, 차단된 친구 기능 추가를 할때마다  
엔티티에 필드를 추가해야하므로, 친구 관련된 엔티티를 따로 만들어 추가  

### 이미지 서버
Go로 된 이미지 서버를 구동 하려고 계획 하였으나,
스프링 부트를 통해 개발 하는것을 고민중  
멀티 모듈을 통해 다른 포트로 서버 구동 가능  

### 인증 서버
인증 토큰 발급 및 토큰 필터링은 스프링 시큐리티를 이용해서 사용중  
스프링 클라우드로 api 게이트 웨이를 이용하여, 별도의 인증서버에서 토큰 발급 및 인증  
redis를 활용한 캐시 서버를 활용해 인증시에 발생하는 부하 감소   

### 채팅 서버

웹 소켓 관련된 부분만을 처리하는 서버가 트래픽이 높아지면 필요할 것으로 보인다.  
웹소켓만을 처리하는 별도의 파드 구성이 필요할것으로 보인다.  
단일간의 채팅 데이터에 대한 동기화를 어떻게 해야할지 고민중이다.  
커넥션을 관리하는 서버를 따로 하위에 두면 부하가 감소될지 검증이 필요하다.  

### 데이터 베이스
개발 환경, 테스트 환경에서는 동일한 데이터 베이스를 사용하고 있음  
운영 환경에서는 각각의 파트에 서로 다른 데이터 베이스에 접근 가능  