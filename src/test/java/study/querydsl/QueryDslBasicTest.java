package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {
    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;


    @BeforeEach
    void init(){
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
    void startJPQL(){
        //member1 찾기
        Member member = em.createQuery("select m from Member m where m.username=:username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(member.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQueryDsl(){

        QMember m = new QMember("m");

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQueryDsl2(){
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void search(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void searchAndParam(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void resultFetch(){
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        /*Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();
*/
        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();
        results.getTotal();

        List<Member> members = results.getResults();

        long total = queryFactory
                .selectFrom(member)
                .fetchCount();

    }

    /**
     * 1. 나이 내림차순
     * 2. 이름 오름차순
     * 3. 2에서 이름이 null이면 마지막에 출력
     */
    @Test
    void sort(){
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();
        Member member5 = fetch.get(0);
        Member member6 = fetch.get(1);
        Member member7 = fetch.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(member7.getUsername()).isNull();
    }

    @Test
    void paging1(){
        List<Member> members = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(members.size()).isEqualTo(2);
    }

    @Test
    void paging2(){
        QueryResults<Member> members = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(members.getTotal()).isEqualTo(4);
        assertThat(members.getResults().size()).isEqualTo(2);
    }

    @Test
    void aggregation(){
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member).fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령
     */
    @Test
    void groupBy(){
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

    @Test
    void join(){
        List<Member> members = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(members).extracting("username")
                .containsExactly("member1","member2");
    }

    @Test
    void theta_join(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> members = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(members).extracting("username")
                .containsExactly("teamA","teamB");
    }

    @Test
    void joinOnFiltering(){
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA")).fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }



    @Test
    void joinOnNoRelation(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));


        List<Tuple> members = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : members) {
            System.out.println("tuple = " + tuple);
        }

    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    void fetchJoinNo(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).isFalse();

    }

    @Test
    void fetchJoin(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).isTrue();

    }


    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    void subQuery(){
        QMember subM = new QMember("subM");


        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(subM.age.max())
                                .from(subM)
                ))
                .fetch();

        assertThat(members).extracting("age").containsExactly(40);

    }

    @Test
    void subQueryGoe(){
        QMember subM = new QMember("subM");

        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(subM.age.avg())
                                .from(subM)
                ))
                .fetch();

        assertThat(members).extracting("username").containsExactly("member3","member4");

    }

    @Test
    void subQueryIn(){
        QMember subM = new QMember("subM");

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(subM.age)
                                .from(subM)
                                .where(subM.age.goe(20))
                ))
                .fetch();
        assertThat(fetch.size()).isEqualTo(3);

    }

    @Test
    void selectSubQuery(){
        QMember subM = new QMember("subM");

        List<Tuple> members = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(subM.age.avg())
                                .from(subM))
                .from(member)
                .fetch();

        for (Tuple tuple : members) {
            System.out.println("tuple = " + tuple);

        }
    }

    @Test
    void basicCase(){
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("10살")
                        .when(20).then("20살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    @Test
    void complexCase(){
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20")
                        .when(member.age.between(21, 30)).then("21~30")
                        .otherwise("기타")
                ).from(member).fetch();

        for (String s : result) {
            System.out.println("s = " + s);

        }
    }

    @Test
    void constant(){
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void concat(){
        List<String> fetch = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void simpleProjection(){
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void tupleProjection(){
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple.get(member.username) = " + tuple.get(member.username));
            System.out.println("tuple.get(member.age) = " + tuple.get(member.age));
        }
    }


    @Test
    void findDtoByJPQL(){
        List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username,m.age)" +
                " from Member m", MemberDto.class).getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto.getUsername() = " + memberDto.getUsername());
            System.out.println("memberDto.getAge() = " + memberDto.getAge());
        }
    }

    @Test
    void findDtoQueryDsl(){
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    //@Test
    void findDtoQueryDslByField(){
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    @Test
    void findDtoQueryDslByConstruct(){
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    @Test
    void findDtoQueryDslByConstruct2(){
        QMember m1 = new QMember("m1");
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(JPAExpressions
                                    .select(m1.age.max())
                                    .from(m1),"age")
                ))
                .from(member)
                .fetch();
    }

    @Test
    void findDtoByQueryProjection(){
        List<MemberDto> members = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : members) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void dynamicQuery_BooleanBuilder(){
        String usernameParam="member1";
        Integer ageParam=10;

        List<Member> result=searchMember1(usernameParam,null);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameParam, Integer ageParam) {

        BooleanBuilder builder=new BooleanBuilder();

        if(usernameParam !=null){
            builder.and(member.username.eq(usernameParam));
        }

        if(ageParam!=null){
            builder.and(member.age.eq(ageParam));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch()    ;
    }

    @Test
    void dynamicQuery_WhereParameter(){
        String usernameParam="member1";
        Integer ageParam=10;

        List<Member> result=searchMember2(usernameParam,ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameParam, Integer ageParam) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameParam),ageEq(ageParam))
                .fetch();
    }

    private BooleanExpression ageEq(Integer ageParam) {
        if(ageParam==null) return null;
        return member.age.eq(ageParam);
    }

    private BooleanExpression usernameEq(String usernameParam) {
        if(usernameParam!=null)return member.username.eq(usernameParam);
        return null;
    }

    private Predicate allEq(String usernameParam,Integer ageParam){
        return usernameEq(usernameParam).and(ageEq(ageParam));
    }

    @Test
    void bulkUpdate(){
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();

        assertThat(count).isEqualTo(2);
    }

    @Test
    void bulkAdd(){
        queryFactory
                .update(member)
                .set(member.age,member.age.add(1))
                .execute();
    }


    @Test
    void bulkMultiply(){
        queryFactory
                .update(member)
                .set(member.age,member.age.multiply(1))
                .execute();
    }

    @Test
    void bulkDelete(){
        queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    @Test
    void sqlFunction(){
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate
                                ("function('replace',{0},{1},{2})"
                                        , member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void sqlFunction2(){
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(member.username.lower()))
                .fetch();
    }



}
