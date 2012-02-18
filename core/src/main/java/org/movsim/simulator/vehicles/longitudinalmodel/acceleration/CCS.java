package org.movsim.simulator.vehicles.longitudinalmodel.acceleration;

import org.movsim.input.model.vehicle.longitudinalmodel.LongitudinalModelInputDataCCS;
import org.movsim.simulator.vehicles.Vehicle;
import org.movsim.simulator.vehicles.longitudinalmodel.LongitudinalModelBase;
import org.movsim.utilities.MyRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CCS extends LongitudinalModelBase {

    final static Logger logger = LoggerFactory.getLogger(CCS.class);

    final static private double DENSITY_AIR = 1.3;
    final static private double EARTH_GRAVITY = 9.81;

    private double mass;
    private double A;
    private double cw;
    private double friction;
    private double T;
    private double p0;
    private double v_c;
    private double p_herringbone;
    private double v_c_herringbone;
    private double b;
    private double b_maximal;

    private double lenght;

    private int gradient;

    /**
     * Instantiates a new CCS (cross country skiing).
     * 
     * @param parameters
     *            the parameters
     * @param vehLength
     */
    public CCS(LongitudinalModelInputDataCCS parameters, double vehLength) {
        super(ModelName.CCS, parameters);
        lenght = vehLength;
        mass = parameters.getMass();
        A = parameters.getA();
        cw = parameters.getCw();
        friction = parameters.getFriction();
        T = parameters.getT();
        p0 = parameters.getP0();
        v_c = parameters.getV_c();
        p_herringbone = parameters.getP_herringbone();
        v_c_herringbone = parameters.getV_c_herringbone();
        b = parameters.getB();
        b_maximal = parameters.getB_maximal();
        s0 = parameters.getS0();
    }

    /**
     * Sets the relative randomization v0.
     * 
     * @param relRandomizationFactor
     *            the new relative randomization v0
     */
    @Override
    public void setRelativeRandomizationV0(double relRandomizationFactor) {
        final double equalRandom = 2 * MyRandom.nextDouble() - 1; // in [-1,1]
        final double newP0 = p0 * (1 + relRandomizationFactor * equalRandom);
        logger.debug("randomization of power: p0={}, new p0={}", p0, newP0);
        setP0(newP0);
    }

    private void setP0(double newP0) {
        this.p0 = newP0;
    }

    @Override
    public double calcAcc(Vehicle me, final Vehicle frontVehicle) {
        // Local dynamical variables
        final double s = me.getNetDistance(frontVehicle);
        final double v = me.getSpeed();
        final double dv = me.getRelSpeed(frontVehicle);

        // System.out.println("v," +v+" gradient "+gradient );

        return acc(s, v, dv, gradient);
    }

    @Override
    public double calcAccSimple(double s, double v, double dv) {
        return acc(s, v, dv, gradient);
    }

    private double acc(double s, double v, double dv, double gradient) {

        double a_max = 4 * p0 / (v_c * mass);
        double gradientSlip = a_max / EARTH_GRAVITY;

        double F_diagonal = (4 * p0 / v_c) * (1 - v / v_c) * ((v < v_c) ? 1 : 0);
        double F_herringbone = (4 * p_herringbone / v_c_herringbone) * (1 - v / v_c) * ((v < v_c_herringbone) ? 1 : 0);

        double F = (gradient < 0.5 * gradientSlip) ? F_diagonal : Math.max(F_diagonal, F_herringbone);

        double b_kin = 0.5 * v * dv * ((dv > 0) ? 1 : 0) / Math.max(s - s0, 0.00001 * s0);

        double acc_free = F / mass - 0.5 * cw * A * DENSITY_AIR * v * v / mass - EARTH_GRAVITY * (friction + gradient);

        double s_rel = (v * T + 0.5*s0) / Math.max(s - 0.5*s0, 0.00001 * s0);
        double acc_int = -(b_kin * b_kin / b) - Math.max(b * (s_rel - 1), 0) - Math.max(acc_free * s_rel, 0);
        double aWanted = Math.max(acc_free + acc_int, -b_maximal - gradient * EARTH_GRAVITY);
        // if(s<2) { System.out.println("s: "+s+ "s0:  "+s0+"  s_rel: "+
        // s_rel+"   v:  "+v+"   afree: "+acc_free+"    acc_int: "+acc_int+ "   awanted: "+ aWanted);
        // }
        logger.debug("aWanted = {}", aWanted);
        return aWanted;
    }
    
    @Override
    public double calcAcc(Vehicle me, Vehicle frontVehicle, double alphaT, double alphaV0, double alphaA) {

        // Local dynamical variables
        final double s = me.getNetDistance(frontVehicle);
        final double v = me.getSpeed();
        final double dv = me.getRelSpeed(frontVehicle);

        gradient = 0;
        return acc(s, v, dv, gradient);
    }
    
    @Override
    public double getDesiredSpeed() {
        throw new UnsupportedOperationException("getDesiredSpeed not applicable for CSS model.");
    }
}