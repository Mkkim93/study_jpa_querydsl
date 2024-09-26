package study.querydsl.repository;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import study.querydsl.entity.Member;
import study.querydsl.repository.support.Querydsl4RepositorySupport;

@Repository
public class MemberTestRepository extends Querydsl4RepositorySupport {

    // TODO
    public MemberTestRepository(EntityManager em) {
        super(em);
    }
}
