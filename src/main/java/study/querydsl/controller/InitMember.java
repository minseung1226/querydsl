package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;

@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

    private final InitMemberService memberService;

    @PostConstruct
    public void init(){
        memberService.init();
    }
    @Component
    static class InitMemberService{
        @Autowired
        EntityManager em;

        @Transactional
        public void init(){
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            em.persist(teamB);
            em.persist(teamA);

            for(int i=0;i < 100;i++){
                Team selectedTeam= i%2==0?teamA:teamB;
                em.persist(new Member("member"+i,i,selectedTeam));
            }
        }
    }

}
