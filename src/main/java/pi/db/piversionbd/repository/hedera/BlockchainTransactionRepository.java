package pi.db.piversionbd.repository.hedera;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.hedera.BlockchainTransaction;

import java.util.List;

@Repository
public interface BlockchainTransactionRepository extends JpaRepository<BlockchainTransaction, Long> {

    List<BlockchainTransaction> findByMember_IdOrderByCreatedAtDesc(Long memberId);

    @Modifying
    @Query("DELETE FROM BlockchainTransaction b WHERE b.claimId = :claimId")
    void deleteByClaimId(@Param("claimId") Long claimId);
}
