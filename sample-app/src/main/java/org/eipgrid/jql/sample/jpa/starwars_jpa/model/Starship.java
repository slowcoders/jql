package org.eipgrid.jql.sample.jpa.starwars_jpa.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;


@Entity
@Table(name = "starship", schema = "starwars_jpa", catalog = "starwars_jpa",
        uniqueConstraints = {
                @UniqueConstraint(name ="starship_pkey", columnNames = {"id"})
        }
)
public class Starship implements java.io.Serializable {
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;


    @Getter @Setter
    @Column(name = "length", nullable = true)
    private Float length;

    @Getter @Setter
    @Column(name = "name", nullable = false)
    private String name;

    @Getter @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pilot_id", nullable = true, referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_pilot_id_2_pk_character__id"))
    private Character pilot;

}
