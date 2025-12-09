package pandq.domain.models.branch;

import jakarta.persistence.*;
import lombok.*;
import pandq.domain.models.enums.BranchStatus;

import java.util.UUID;

@Entity
@Table(name = "branches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    private String phone;

    private Double latitude;
    private Double longitude;

    private String openingHours;

    @Enumerated(EnumType.STRING)
    private BranchStatus status;
}
