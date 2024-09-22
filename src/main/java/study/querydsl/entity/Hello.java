package study.querydsl.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import static jakarta.persistence.GenerationType.*;

@Entity
@Getter @Setter
public class Hello {

    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;
}
