package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDTO {

    private String username;
    private int age;

    @QueryProjection // Dto 를 Q 파일로 등록하기 위한 QueryProjection 어노테이션
    public MemberDTO(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
