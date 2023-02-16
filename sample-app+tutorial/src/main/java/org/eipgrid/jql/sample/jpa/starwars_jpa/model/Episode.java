package org.eipgrid.jql.sample.jpa.starwars_jpa.model;

import lombok.Getter;
import lombok.Setter;
import java.util.*;
import javax.persistence.*;


@Entity
@Table(name = "episode", schema = "starwars_jpa", catalog = "starwars_jpa",
        uniqueConstraints = {
                @UniqueConstraint(name ="episode_pkey", columnNames = {"title"})
        }
)
public class Episode implements java.io.Serializable {
    @Getter @Setter
    @Id
    @Column(name = "title", nullable = false)
    private String title;

    @Getter @Setter
    @Column(name = "published", nullable = true, columnDefinition = "timestamp")
    private java.sql.Timestamp published;

    @Getter @Setter
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "character_episode_link", schema = "starwars_jpa", catalog = "starwars_jpa",
            uniqueConstraints = {
                    @UniqueConstraint(name ="character_id__episode_id__uindex", columnNames = {"character_id", "episode_id"})
            },
            joinColumns = @JoinColumn(name="episode_id"), inverseJoinColumns = @JoinColumn(name="character_id"))
    private List<Character> character_;

}
