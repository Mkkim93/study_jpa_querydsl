package study.querydsl.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDTO;

import java.util.List;

public interface MemberRepositoryCustom {

    List<MemberTeamDTO> search(MemberSearchCondition condition);
    Page<MemberTeamDTO> searchPageSimple(MemberSearchCondition condition, Pageable pageable); // 단순한 페이징 쿼리 구현
    Page<MemberTeamDTO> searchPageComplex(MemberSearchCondition condition, Pageable pageable); // 카운트 쿼리와 페이징 쿼리 별도로 구현
}
