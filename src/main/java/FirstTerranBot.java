import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.Ability;
import com.github.ocraft.s2client.protocol.data.UnitType;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.spatial.Point;
import com.github.ocraft.s2client.protocol.spatial.Point2d;
import com.github.ocraft.s2client.protocol.spatial.PointI;
import com.github.ocraft.s2client.protocol.unit.Alliance;
import com.github.ocraft.s2client.protocol.unit.Unit;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class FirstTerranBot extends S2Agent {


    List<UnitInPool> barracks;
    List<UnitInPool> scvs;
    List<UnitInPool> marines;
    Point2d enemyBasePoind2d;
    List<UnitInPool> comandCenters;
    boolean weGoAttack = false;

    public void onGameStart() {
        System.out.println("Hello world of Starcraft II bots!");
        knowMyPlace();
        countMyUnits();
    }

    public void onStep() {
        countMyUnits();
        tryBuildSupplyDepot();
        tryBuildBarack();
        attackWithMarines();
    }

    public void knowMyPlace() {
        comandCenters = observation().getUnits
                (Alliance.SELF, UnitInPool.isUnit(Units.TERRAN_COMMAND_CENTER));
        Point myPos = comandCenters.get(0).unit().getPosition();
        PointI center = observation().getGameInfo().findCenterOfMap();
        int targetX = 0;
        int targetY = 0;
        if (myPos.getX() < center.getX()) {
            targetX = Properties.RIGHT_X;
        } else {
            targetX = 0;
        }
        if (myPos.getY() < center.getY()) {
            targetY = Properties.TOP_Y;
        } else {
            targetY = 0;
        }
        enemyBasePoind2d = Point2d.of(targetX, targetY);
//            observation().getGameInfo().
    }

    public void countMyUnits() {
        barracks = observation().getUnits
                (Alliance.SELF, UnitInPool.isUnit(Units.TERRAN_BARRACKS));
        scvs = observation().getUnits
                (Alliance.SELF, UnitInPool.isUnit(Units.TERRAN_SCV));
        marines = observation().getUnits
                (Alliance.SELF, UnitInPool.isUnit(Units.TERRAN_MARINE));
        comandCenters = observation().getUnits
                (Alliance.SELF, UnitInPool.isUnit(Units.TERRAN_BARRACKS));
    }

    public void onUnitIdle(UnitInPool unitInPool) {
        Unit unit = unitInPool.unit();
        switch ((Units) unit.getType()) {
            case TERRAN_COMMAND_CENTER:
                if (scvs.size() < Properties.SCV_NEED_NUMBER)
                    actions().unitCommand(unit, Abilities.TRAIN_SCV, false);
                break;
            case TERRAN_SCV:
                findNearestMineralPatch(unit.getPosition().toPoint2d()).ifPresent(mineralPath ->
                        actions().unitCommand(unit, Abilities.SMART, mineralPath, false));
                break;
            case TERRAN_BARRACKS:
                actions().unitCommand(unit, Abilities.TRAIN_MARINE, false);
                break;
            case TERRAN_MARINE:

                if (unit.getPosition().toPoint2d().distance(enemyBasePoind2d) < Properties.DISTANCE_TO_ENEMY_BASE_TO_PATROL) {
                    Point2d randomNearPoint = Point2d.of(unit.getPosition().getX() + new Random().nextInt(6) - 3,
                            unit.getPosition().getY() + new Random().nextInt(6) - 3);
                    actions().unitCommand(unit, Abilities.ATTACK, randomNearPoint, false);
                } else if (weGoAttack) {
                    actions().unitCommand(unit, Abilities.ATTACK, enemyBasePoind2d, false);

                }
            default:
                break;
        }
    }

    private boolean attackWithMarines() {
        if (marines.size() < 50 || weGoAttack) {
            return false;
        }
        weGoAttack = true;
        for (UnitInPool marine : marines) {

            actions().unitCommand(marine.unit(), Abilities.ATTACK, enemyBasePoind2d, false);
        }


        // Try and build a depot. Find a random TERRAN_SCV and give it the order.
        return tryBuildStructure(Abilities.BUILD_SUPPLY_DEPOT, Units.TERRAN_SCV);
    }

    private boolean tryBuildSupplyDepot() {
        // If we are not supply capped, don't build a supply depot.
        if (observation().getFoodUsed() <= observation().getFoodCap() - Properties.FOOD_GAP) {
            return false;
        }

        // Try and build a depot. Find a random TERRAN_SCV and give it the order.
        return tryBuildStructure(Abilities.BUILD_SUPPLY_DEPOT, Units.TERRAN_SCV);
    }

    private boolean tryBuildBarack() {

        List<UnitInPool> barracks = observation().getUnits
                (Alliance.SELF, UnitInPool.isUnit(Units.TERRAN_BARRACKS));

        if (barracks.size() > Properties.BARACKS_NEED_NUMBER) {
            return false;
        }

        // Try and build a depot. Find a random TERRAN_SCV and give it the order.
        return tryBuildStructure(Abilities.BUILD_BARRACKS, Units.TERRAN_SCV);
    }

    private boolean tryBuildStructure(Ability abilityTypeForStructure, UnitType unitType) {
        // If a unit already is building a supply structure of this type, do nothing.
        if (!observation().getUnits(Alliance.SELF, doesBuildWith(abilityTypeForStructure)).isEmpty()) {
            return false;
        }

        // Just try a random location near the unit.
        Optional<UnitInPool> unitInPool = getRandomUnit(unitType);
        if (unitInPool.isPresent()) {
            Unit unit = unitInPool.get().unit();
            actions().unitCommand(
                    unit,
                    abilityTypeForStructure,
                    unit.getPosition().toPoint2d().add(Point2d.of(getRandomScalar(), getRandomScalar()).mul(15.0f)),
                    false);
            return true;
        } else {
            return false;
        }

    }

    private Predicate<UnitInPool> doesBuildWith(Ability abilityTypeForStructure) {
        return unitInPool -> unitInPool.unit()
                .getOrders()
                .stream()
                .anyMatch(unitOrder -> abilityTypeForStructure.equals(unitOrder.getAbility()));
    }

    private Optional<UnitInPool> getRandomUnit(UnitType unitType) {
        List<UnitInPool> units = observation().getUnits(Alliance.SELF, UnitInPool.isUnit(unitType));
        return units.isEmpty()
                ? Optional.empty()
                : Optional.of(units.get(ThreadLocalRandom.current().nextInt(units.size())));
    }

    private float getRandomScalar() {
        return ThreadLocalRandom.current().nextFloat() * 2 - 1;
    }


    private Optional<Unit> findNearestMineralPatch(Point2d start) {
        List<UnitInPool> units = observation().getUnits(Alliance.NEUTRAL);
        double distance = Double.MAX_VALUE;
        Unit target = null;
        for (UnitInPool unitInPool : units) {
            Unit unit = unitInPool.unit();
            if (unit.getType().equals(Units.NEUTRAL_MINERAL_FIELD)) {
                double d = unit.getPosition().toPoint2d().distance(start);
                if (d < distance) {
                    distance = d;
                    target = unit;
                }
            }
        }
        return Optional.ofNullable(target);
    }


}