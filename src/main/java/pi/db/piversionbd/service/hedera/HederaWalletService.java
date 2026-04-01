package pi.db.piversionbd.service.hedera;

import org.springframework.stereotype.Service;
import pi.db.piversionbd.config.HederaProperties;

/**
 * Creates and manages virtual wallets for members.
 * For MVP: generates virtual wallet ID. Future: create real Hedera accounts.
 */
@Service
public class HederaWalletService {

    private static final String WALLET_PREFIX = "solidari-member-";
    private static final float COINS_PER_DT = 1f / 3f; // 1 coin = 3 DT

    private final HederaProperties properties;

    public HederaWalletService(HederaProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates a virtual wallet for the member. Returns wallet address (ID).
     */
    public String createWallet(Long memberId) {
        if (!properties.isEnabled()) {
            return null;
        }
        return WALLET_PREFIX + memberId;
    }

    /**
     * Converts DT (Dinars Tunisiens) to coins. 1 coin = 3 DT.
     */
    public static float dtToCoins(float dtAmount) {
        return dtAmount * COINS_PER_DT;
    }

    /**
     * Converts coins to DT. 1 coin = 3 DT.
     */
    public static float coinsToDt(float coins) {
        return coins * 3f;
    }
}
