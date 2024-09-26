package study.querydsl.repository.support;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDTO;
import study.querydsl.dto.QMemberTeamDTO;
import study.querydsl.entity.Member;
import study.querydsl.repository.MemberRepositoryCustom;

import java.util.List;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

public class Querydsl4RepositorySupport extends org.springframework.data.jpa.repository.support.QuerydslRepositorySupport implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public Querydsl4RepositorySupport(EntityManager em) {
        super(Member.class);
        queryFactory = new JPAQueryFactory(em);
    }

    // 페이징 쿼리 (offset(), limit() ) 추가
    @Override
    public Page<MemberTeamDTO> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {

        QueryResults<MemberTeamDTO> result = queryFactory
                // .selectFrom(member)
                .select(new QMemberTeamDTO(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset()) // 몇번째 부터 시작할 것인지
                .limit(pageable.getPageSize()) // 하나의 페이지에 몇개를 가져올 것인가?
                .fetchResults(); // content 용 쿼리와 카운트 쿼리 두번 날림
        // fetchResults() : 위의 쿼리의 카운트 쿼리를 동시에 수행할 때 카운트 쿼리도 조인을 진행함
        // 요구사항에 따라 카운트쿼리는 조인이 없어도 정상적으로 카운트에 진행이 가능할 때 비효율적
        // 그래서 아래의 SimpleComplex() 메서드를 구현하여 카운트 쿼리를 직접구현한다 (직접 구현한 카운트 쿼리는 join 을 제거할 수 있음)
        List<MemberTeamDTO> content = result.getResults();
        long totalCount = result.getTotal();

        return new PageImpl<>(content, pageable, totalCount);
    }

    public Page<MemberTeamDTO> searchPageSimple2(MemberSearchCondition condition, Pageable pageable) {
        JPQLQuery<MemberTeamDTO> jpaQuery = from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .select(new QMemberTeamDTO(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")));
                /*.offset(pageable.getOffset())
                .limit(pageable.getPageSize()) */

        JPQLQuery<MemberTeamDTO> query = getQuerydsl().applyPagination(pageable, jpaQuery);
        return (Page<MemberTeamDTO>) query.fetchAll();
    }

    @Override
    public Page<MemberTeamDTO> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDTO> content = queryFactory
                // .selectFrom(member)
                .select(new QMemberTeamDTO(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName"))
                )
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset()) // 몇번째 부터 시작할 것인지
                .limit(pageable.getPageSize()) // 하나의 페이지에 몇개를 가져올 것인가?
                .fetch();
        JPAQuery<Member> countQuery = queryFactory // 직접 totalCount 쿼리를 구현 이유 : join 이 필요 없는 경우가 있을 수 있기 때문에
                .selectFrom(member)
                .leftJoin(member.team, team) // 카운트 쿼리를 직접 구현할 때 커스텀 가능 (조인 제거)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );
                // .fetch 생략 가능
                // 아래의 PageableExecutionUtils()
                // 기능 : 전체 페이지 보다 컨텐츠 수가 적을 때 카운트 쿼리를 날리지 않고
        return PageableExecutionUtils.getPage(content, pageable, () -> countQuery.fetchCount());
    }

    @Override
    public List<MemberTeamDTO> search(MemberSearchCondition condition) {

        // querySupport 적용 쿼리 (querySupport 내부에 EntityManager 가 있음)
        List<MemberTeamDTO> result = from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .select(new QMemberTeamDTO(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .fetch();

        /*return queryFactory
                // .selectFrom(member)
                .select(new QMemberTeamDTO(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .fetch();*/
        return result;
    }

    private BooleanExpression ageBetween(int ageLoe, int ageGoe) {
        return ageGoe(ageLoe).and(ageGoe(ageGoe));
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.eq(ageLoe) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return teamName != null ? team.name.eq(teamName) : null;
    }

    private BooleanExpression usernameEq(String username) {
        return username != null ? member.username.eq(username) : null;
    }
}
