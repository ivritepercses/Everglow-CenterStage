package org.EverglowLibrary.Systems;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.signum;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.Servo;

public class FourBarSystem{

    public enum Level {
        START(35), PICKUP(35), DROP(-635), REST(-635), LOW(-410); //344
        //start: -10, pickup: 210,235
        public final int state;

        Level(int state) {
            this.state = state;
        }
    }

    public enum ServoAngel {
        PICKUP(0.56), DROP(0), REST(1), LOW(1);

        public final double state;

        ServoAngel(double state) {
            this.state = state;
        }
    }

    Level currentLevel = Level.PICKUP;
    ServoAngel currentServoAngel = ServoAngel.PICKUP;

    DcMotorEx fourBarMotor;
    LinearOpMode opMode;
    Servo clawAngelServo;

    double fourBarTarget = 0;

    public FourBarSystem(LinearOpMode opMode){

        this.opMode = opMode;
        fourBarMotor = opMode.hardwareMap.get(DcMotorEx .class, "4Bar");
        clawAngelServo = opMode.hardwareMap.get(Servo.class, "FlipServo");
        clawAngelServo.setPosition(ServoAngel.PICKUP.state);

        //fourBarMotor.setDirection( DcMotorSimple.Direction.REVERSE);

        //fourBarMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        fourBarMotor.setTargetPosition(Level.START.state);
        fourBarMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        PIDFCoefficients pid = fourBarMotor.getPIDFCoefficients(DcMotor.RunMode.RUN_TO_POSITION);
        pid.i = 1.5;
        pid.p = 40;
        pid.d = 5;
        pid.f = 1;

        fourBarMotor.setPIDFCoefficients(DcMotor.RunMode.RUN_TO_POSITION, pid);

        //clawAngelServo.resetDeviceConfigurationForOpMode();
    }

    public void restart(){
        fourBarMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        fourBarMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
    }
    public boolean isFinish(Level level){
        boolean isFinished;
        final int epsilon4Bar = 15;
        final int lower = -318;
        final int upper =  -200;
        isFinished = ((fourBarMotor.getCurrentPosition() >= level.state - epsilon4Bar) &&
                (fourBarMotor.getCurrentPosition() <= level.state + epsilon4Bar));

        if(level == Level.DROP){
            isFinished = isFinished || fourBarMotor.getCurrentPosition() <= lower;
        }
        else if(level == Level.PICKUP){
            isFinished = isFinished || fourBarMotor.getCurrentPosition() >= upper;
        }

        opMode.telemetry.addData("is FourBar finished? ", isFinished);
        return isFinished;
    }

    public Level getTargetPosition(){
        return currentLevel;
    }
    public DcMotorEx getFourBarMotor(){
        return this.fourBarMotor;
    }

    public int getCurrentMotorPosition() {
        return fourBarMotor.getCurrentPosition();
    }

    public double getCurrentServoPosition() {
        return clawAngelServo.getPosition();
    }

    public void setMotorPower(double power) {
        fourBarMotor.setPower(power);
    }

    public void setServoPosition(double position) {
        clawAngelServo.setPosition(position);
    }

    public void set4BarPosition(int target) {
        fourBarTarget = target;
        fourBarMotor.setTargetPosition(target);
//        if (fourBarTarget == Level.DROP.state && !isFinish(Level.DROP)) {
//            currentLevel = Level.DROP;
//            fourBarMotor.setTargetPosition(target+75);
//            opMode.telemetry.addLine("To virtual");
//        } else {
//            fourBarMotor.setTargetPosition(target);
//            opMode.telemetry.addLine("To actual");
//        }
    }
    public void set4BarPositionByLevel(Level targetLevel) {
        currentLevel = targetLevel;
        set4BarPosition(currentLevel.state);
    }

    public void setServoPositionByLevel(ServoAngel targetServoAngel) {
        currentServoAngel = targetServoAngel;
        clawAngelServo.setPosition(currentServoAngel.state);
    }

    public void toggle4Bar() {
        if(currentLevel == Level.START || currentLevel == Level.PICKUP || currentLevel == Level.REST) {
            set4BarPositionByLevel(Level.DROP);
        }
        else{
            set4BarPositionByLevel(Level.PICKUP);
        }
    }

    public Level getTargetLevel() {
        return currentLevel;
    }

    public void toggleAngleServo() {
        if(currentLevel == Level.START || currentLevel == Level.PICKUP) {
            setServoPositionByLevel(ServoAngel.DROP);
        }
        else{
            setServoPositionByLevel(ServoAngel.PICKUP);
        }
    }

    double last = 0;
    double change = 0;

    public void updateP(double modifairG) {
        final double mod2 = 0.15;
        final double restGravityPosition = 34;
        change = getCurrentMotorPosition() - last;
        last = getCurrentMotorPosition();
        double deviation = fourBarTarget - getCurrentMotorPosition();
        double motorPower = -deviation / 70;

        double AngleGravity = (getCurrentMotorPosition() - restGravityPosition) / 270 * PI;
        double gravityPower =  modifairG * Math.sin(AngleGravity);
        motorPower += gravityPower - mod2 * change;

        //opMode.telemetry.addData("Target", fourBarTarget);
        //opMode.telemetry.addData("deviation", deviation);
        //opMode.telemetry.addData("motorPower", motorPower);
        //if(deviation > 100) motorPower += modifair;
        //if(deviation < 20) motorPower -= modifair;
        setMotorPower(motorPower);
    }

    public Executor getExecutor(Level level,ServoAngel servoAngel){
        return new FourBarExecutor(servoAngel,level);
    }

    public Executor getExecutor(Level level,ServoAngel servoAngel, double power){
        return new FourBarExecutor(servoAngel,level, power);
    }


    public class FourBarExecutor extends Executor{

        private final ServoAngel m_ServoAngle;
        private final Level m_Level;
        private final double m_Power;

        public FourBarExecutor(ServoAngel servoAngel, Level level) {
            m_ServoAngle = servoAngel;
            m_Level = level;
            m_Power = 0.85;
        }

        public FourBarExecutor(ServoAngel servoAngel, Level level, double power) {
            m_ServoAngle = servoAngel;
            m_Level = level;
            m_Power = power;
        }

        @Override
        public void run() {
            if(m_Level == Level.PICKUP){
                fourBarMotor.setPower(m_Power);
                set4BarPositionByLevel(m_Level);
                //opMode.sleep(400);
                setServoPositionByLevel(m_ServoAngle);
            }
            else {
                fourBarMotor.setPower(m_Power);
                setServoPositionByLevel(m_ServoAngle);
                set4BarPositionByLevel(m_Level);
            }
        }


        @Override
        public boolean isFinished() {
            //set4BarPositionByLevel(m_Level);
            return isFinish(m_Level);
        }

        @Override
        public void stop() {
            fourBarMotor.setPower(0);
        }


    }
}