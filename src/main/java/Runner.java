import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.S2Coordinator;
import com.github.ocraft.s2client.protocol.game.BattlenetMap;
import com.github.ocraft.s2client.protocol.game.Race;

public class Runner extends  Object{


    public static void main(String[] args) {
        S2Agent bot = new FirstTerranBot();
        S2Coordinator s2Coordinator = S2Coordinator.setup()
                .loadSettings(args).setRealtime(true)
                .setParticipants(
                        S2Coordinator.createParticipant(Race.TERRAN, new PassiveBot()),
                        S2Coordinator.createParticipant(Race.TERRAN, bot))
                        //S2Coordinator.createComputer(Race.ZERG, Difficulty.VERY_HARD))
//                        S2Coordinator.createComputer(Race.TERRAN,Difficulty.VERY_HARD))
                .launchStarcraft()
                .startGame(BattlenetMap.of("Cloud Kingdom LE"));

        while (s2Coordinator.update()) {
        }

        s2Coordinator.quit();
    }
}
