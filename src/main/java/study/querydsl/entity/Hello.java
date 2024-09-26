package study.querydsl.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static jakarta.persistence.GenerationType.*;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Hello {

    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;
}
