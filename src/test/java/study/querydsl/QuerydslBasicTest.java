package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDTO;
import study.querydsl.dto.QMemberDTO;
import study.querydsl.dto.UserDTO;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        // member1을 찾아라
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {

        QMember m = new QMember("m1");
        String member1 = "member1";

        Member findMember = queryFactory.select(m)
                .from(m)
                .where(m.username.eq(member1))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl3() {
        Member findMember = queryFactory.select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"), // and 생략 가능
                        (member.age.eq(10))
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    public void resultFetch() {


List<Member> fetch = queryFactory.selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory.selectFrom(member).fetchOne();

        Member fetchFirst = queryFactory.selectFrom(member)
                .fetchFirst();


        // paging 용 쿼리
        QueryResults<Member> results = queryFactory.selectFrom(member)
                .fetchResults();

        results.getTotal();
        List<Member> content = results.getResults();

        long total = queryFactory.selectFrom(member)
                .fetchCount();

        System.out.println("results = " + results.getResults());

        System.out.println("total = " + total);
    }



    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단, 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */


    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() {
        List<Member> result = queryFactory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults()).size().isEqualTo(2);
    }

    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory.select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
    }



    /**
     *
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */

    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

     /**
     *
     * teamA 에 소속된 모든 회원을 찾아라
     */

    @Test
    public void join() throws Exception {

        List<Member> result = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");

        // 1. extracting("username"):
        //extracting 메서드는 result 리스트 안에 있는 객체들에서 특정 필드의 값을 추출합니다. 이 경우,
        // Member 객체의 username 필드를 추출하는 것입니다. 따라서 조인한 결과로 반환된 Member 객체들에서 username 필드만 추출하게 됩니다.

        // 2. containsExactly("member1", "member2"):
        //containsExactly는 추출된 username 값들이 지정된 순서대로 정확히 일치하는지 검증합니다.
        // 즉, extracting으로 추출한 username 값들이 "member1", "member2" 순서대로 나오는지 확인합니다.
    }



    /**
     *
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조인
     */

    @Test
    public void theta_join() throws Exception {

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory.select(member)
                .from(member, team) // 클래스 두개를 나열
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result).extracting("username")
                .containsExactly("teamA", "teamB");
    }



    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA 인 팀만 조인, 회원은 모두 조회
     *  JPQL : select m, t from Member m left join m.team t on t.name = 'teamA'
     */


    @Test
    public void join_on_filtering() throws Exception {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }



    /**
     * 연관관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */


    @Test
    public void join_on_no_relation() throws Exception {

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member) // 클래스 두개를 나열
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() throws Exception {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin() // fetch join 적용 member 와 연관된 team 을 한번에 가져옴
                // join : member.team : Member 엔티티에 있는 team_id 외래키, team : team 엔티티에 있는 pk
                .where(member.username.eq("member1"))
                .fetchOne();
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isTrue();
    }



    /**
     * 나이가 가장 많은 회원 조회
     */


    @Test
    public void subQuery() throws Exception {

        QMember memberSub = new QMember("memberSub"); // 서브쿼리가 될 새로운 QMember 객체 생성
        // QMember 의 인스턴스가 메인쿼리 내부에 들어가는 서브쿼리의 select 대상이 된다. 즉, Member 테이블의 Alias 가 되는 것임

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result)
                .extracting("age")
                .containsExactly(40);
    }



    /**
     * 나이가 평균 이상인 회원
     */


    @Test
    public void subQuery2() throws Exception {

        QMember subMember = new QMember("subMember");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(subMember.age.avg())
                                .from(subMember)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(30, 40);
    }



        /**
     * 서브쿼리 IN
     */


    @Test
    public void subQuery3() throws Exception {
        QMember subMember = new QMember("subMember");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(subMember.age)
                                .from(subMember)
                                .where(subMember.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(20,30, 40);
    }

    @Test
    public void selectSubquery() throws Exception {
        QMember subMember = new QMember("subMember");
        int age = 10;
        List<Tuple> result = queryFactory
                .select(member.username.as("기모띵"),
                        // Alias
                        JPAExpressions.select(subMember.age.avg())
                                .from(subMember))

                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * case 문
     */


    @Test
    public void basicCase() throws Exception {

        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }


    /**
     * case 문 심화
     * @throws Exception
     */

    @Test
    public void complexCase() throws Exception {

        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21살~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void constant() throws Exception {

        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }



    /**
     * concat
     * 만약 붙이려는 데이터가 문자열이 아닌 경우 stringValue() 를 사용 (enumType 값이 안나올 떄 stringValue() 사용하자)
     * concat 두번 써야됨
     * @throws Exception
     */

    @Test
    public void concat() throws Exception {

        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void simpleProjection() throws Exception {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }



    /**
     * select 할 대상이 두개 이상일 때는 return Type = Tuple
     * !중요 : Tuple 로 반환되는 객체는 repository 에서만 사용 할 것 (service 또는 controller 계층까지 넘어가지 않도록 설계)
     */

    @Test
    public void tupleProjection() throws Exception {

        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }



    /**
     *  DTO 로 다이렉트 조회 (new Operation 활용 방법)
     *  step 1. select + new dto 클래스가 있는 경로 설정 (java 파일 바로 하위에 있는 경로부터 dto 클래스까지)
     *  step 2. 경로 설정이 끝나고 MemberDTO 에서 선언한 생성자를 가지고 오듯 파라미터 값을 적는다.
     */

    @Test
    public void findDtoByJPQL() throws Exception {

        List<MemberDTO> resultList = em.createQuery("select new study.querydsl.dto.MemberDTO(m.username, m.age) " +
                                "from Member m", MemberDTO.class)
                .getResultList();

        System.out.println("resultList = " + resultList);
    }



    /**
     * querydsl 사용해서 DTO 에 접근하는 방식 (Projection 'Bean' 접근 방식)
     * 핵심 : 1) select :Projections.bean(조회할 DTO 클래스, 조회할 컬럼 ...)
     */

    @Test
    public void findDtoBy() throws Exception {

        List<MemberDTO> result = queryFactory
                .select(Projections.bean(MemberDTO.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }



    /**
     * querydsl 사용해서 DTO 에 접근하는 방식 (Projection 'field' 접근 방식)
     * 핵심 : 1) select :Projections.field(조회할 DTO 클래스, 조회할 컬럼 ...)
     */

    @Test
    public void findDtoByField() throws Exception {
        List<MemberDTO> result = queryFactory
                .select(Projections.fields(MemberDTO.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    /**
     * querydsl 사용해서 DTO 에 접근하는 방식 (Projection 'constructor' 접근 방식)
     * 핵심 : 1) select :Projections.constructor(조회할 DTO 클래스, 조회할 컬럼 ...)
     * 생성자의 데이터 타입 맞춰줄 것
     */


    @Test
    public void findDtoConstructor() throws Exception {
        List<MemberDTO> result = queryFactory
                .select(Projections.constructor(MemberDTO.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    @Test
    public void findUserDto() throws Exception {
        List<UserDTO> result = queryFactory
                .select(Projections.fields(UserDTO.class,
                        member.username.as("name"), // alias 를 사용하여 UserDTO 의 필드명과 일치 시킨다.
                        member.age))
                .from(member)
                .fetch();

        for (UserDTO userDTO : result) {
            System.out.println("userDTO = " + userDTO);
        }
    }

    @Test
    public void findUserDtoSubquery() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<UserDTO> result = queryFactory
                .select(Projections.fields(UserDTO.class,
                        member.username.as("name"), // alias 를 사용하여 UserDTO 의 필드명과 일치 시킨다.
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max()).from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDTO userDTO : result) {
            System.out.println("userDTO = " + userDTO);
        }
    }

    // 연습 : 생성자 방식
    @Test
    public void soloTestQueryConstructor() throws Exception {

        List<UserDTO> userDto = queryFactory
                .select(Projections.constructor(UserDTO.class,
                        member.username.as("name"),
                        member.age))
                .from(member)
                .fetch();

        for (UserDTO userDTO : userDto) {
            System.out.println("userDTO = " + userDTO);
        }
    }

    @Test
    public void findDtoByQueryProjection() throws Exception {

        List<MemberDTO> result = queryFactory
                .select(new QMemberDTO(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    @Test
    public void findDtoByQueryConstructor() throws Exception {

        // 생성자의 파라미터 값이 고정 되어 있기 때문에 파라미터 형식과 맞지 않는 데이터 입력 시
        // 컴파일 에러를 잡아준다.
        List<MemberDTO> result = queryFactory
                .select(new QMemberDTO(member.username, member.age))
                .from(member)
                .fetch();

        /*queryFactory.select(Projections.constructor(MemberDTO.class,
                member.username, member.age, + memberTeam 와 같은 파라미터 추가하게 되면 런타임 에러))*/
    }

    /**
     * 동적쿼리 BooleanBuilder() 생성
     */
    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception {

        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();

        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQueryWhereParam() throws Exception {

        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);
        // assertThat(result.size()).isEqualTo(1);
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory.selectFrom(member)
                // .where(usernameEq(usernameCond), ageEq(ageCond))
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression ageEq(Integer ageCond) {
       return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
//    @Commit
    // bulk 연산 수행 시 영속성 컨텍스트 flush clear 를 항상 고려
    public void bulkUpdate() throws Exception {

        // member1 = 10 -> 비회원
        // member2 = 20 -> 비회원
        // member3 = 30 -> 유지
        // member4 = 40 -> 유지

        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    public void bulkAdd() throws Exception {

        queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    @Test
    public void bulkMul() throws Exception {

        queryFactory
                .update(member)
                .set(member.age, member.age.multiply(2))
                .execute();
    }

    @Test
    public void bulkUpdateZero() throws Exception {
    // 컬럼의 값을 모두 0으로 만든다!!!!
        queryFactory
                .update(member)
                .set(member.age, (Integer) 0)
                .execute();
    }

    @Test
    public void bulkDelete() throws Exception {

        queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    @Test
    public void sqlFunction() throws Exception {
        // member 라는 단어를 M 으로 바꿔줘는 함수를 적용
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                                member.username, "member", "M"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunction2() throws Exception {
        // 소문자로 바꾸기
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(Expressions.stringTemplate("function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunctionUpper() throws Exception {

        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(member.username.upper()))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}
