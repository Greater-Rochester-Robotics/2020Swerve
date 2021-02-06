/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018-2019 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot.subsystems;

import com.revrobotics.CANSparkMax;
import com.revrobotics.ControlType;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import com.revrobotics.CANEncoder;
import com.revrobotics.CANPIDController;
import com.revrobotics.CANAnalog;

import com.ctre.phoenix.motorcontrol.can.TalonFX;
import com.ctre.phoenix.motorcontrol.TalonFXControlMode;
import com.ctre.phoenix.sensors.CANCoder;
import com.ctre.phoenix.sensors.AbsoluteSensorRange;

import frc.robot.Constants;



/**
 * This is the class containing both motor controllers and all functions needed to run one swerve module.
 */
public class SwerveModule{
    private TalonFX driveMotor;
    private CANCoder rotateSensor;
    private CANSparkMax rotationMotor;
    private CANEncoder rotationEncoder;
    // private CANAnalog rotationSensor;
    private CANPIDController rotatePID;
    private boolean isInverted = false;//this is for a future function

    /**
     * Creates a new SwerveModule object
     * 
     * @param driveMotorID The CAN ID of the SparkMax connected to the drive motor(expecting NEO)
     * @param rotationMotorID The CAN ID of the SparkMax connected to the module rotation motor(expecting NEO 550)
     * @param canCoderID The CAN ID of the rotation sensor 
     */  
    public SwerveModule(int driveMotorID,int rotationMotorID,int canCoderID){
        //TODO:change this to a TalonFX, check all uses of driveMotor for the right syntax
        //TODO:ask Rob if this is done
        driveMotor = new TalonFX(driveMotorID);
        
        rotationMotor = new CANSparkMax(rotationMotorID , MotorType.kBrushless);
        rotationMotor.restoreFactoryDefaults();//reset the motor controller, wipe old stuff
    
        rotateSensor = new CANCoder(canCoderID);
        rotateSensor.configAbsoluteSensorRange(AbsoluteSensorRange.Signed_PlusMinus180);
        // rotationSensor = rotationMotor.getAnalog(CANAnalog.AnalogMode.kAbsolute);
        // rotationSensor.setPositionConversionFactor(Constants.VOLTAGE_TO_RAD_CONV_FACTOR);

        rotationEncoder = rotationMotor.getEncoder();
        rotatePID = rotationMotor.getPIDController();
        rotatePID.setFeedbackDevice(rotationEncoder);

        //set the PID values for the Encoder controlled rotation
        rotatePID.setP(Constants.SWERVE_ROT_P_VALUE);
        rotatePID.setI(Constants.SWERVE_ROT_I_VALUE);
        rotatePID.setD(Constants.SWERVE_ROT_D_VALUE);
        rotatePID.setIZone(Constants.SWERVE_ROT_I_ZONE_VALUE);
        rotatePID.setFF(Constants.SWERVE_ROT_FF_VALUE);
    }

    /**
     * Set the speed of the drive motor
     * 
     * @param value a number between -1.0 and 1.0, where 0.0 is not moving
     */
    public void setDriveMotor(double value){
        driveMotor.set(TalonFXControlMode.PercentOutput,value);//*(isInvertted?-1:0));
    }

    //TODO:create access to the driveMotor encoder count
    public double getDriveDistance(){
        return driveMotor.getSensorCollection().getIntegratedSensorPosition();
    }

    public double getDriveVelocity(){
        return driveMotor.getSensorCollection().getDriveVelocity();
    }

    //TODO:create a reset for the driveMotor encoder

    public void resetEncoder(){
        //rotationEncoder.
    }

    //TODO:create a means of setting the value of the CANCoder(use setPosition of the CANCoder)

    /**
     * @return the position of the module in degrees, should limit from -180 to 180
     */
    public double getPosInDeg(){ 
        //TODO:change to just getPostion from rotationSensor
        return rotateSensor.getAbsolutePosition();
    }

    /**
     * If this is too resource intensive, switch to a periodic call,
     *  and replace with a poll of said variable
     * @return the position of the module in radians, should limit from -PI to PI
     */
    public double getPosInRad(){
        //TODO:call getPosInDeg() and convert by multiplying by PI
        return getPosInDeg()*Constants.DEG_TO_RAD_CONV_FACTOR;//(isInverted?0:Math.PI));
        //TODO:Above has to be checked, if the sensor is positive clockwise, fix(Need Robot)
        // double currentAngle = rotationSensor.getPosition();
        // if(isInverted){
        //     if(currentAngle <= Math.PI){
        //         return currentAngle;
        //     }else{
        //         return Constants.TWO_PI-currentAngle;
        //     }
        // }else{
        //     return rotationSensor.getPosition() - Math.PI;
        // }
    }

    /**
     * this is a function meant for testing by getting the count from
     *  the rotational encoder which is internal to the NEO550.
     * @return the encoder count(no units, naturally just the count)
     */
    public double getEncCount(){
        return rotationEncoder.getPosition();
    }

    /**
     * set the setpoint for the module rotation
     * @param targetPos a value between -PI and PI, PI is counter-clockwise, 0.0 is forward 
     */
    public void setPosInRad(double targetPos){
        double posDiff = targetPos - getPosInRad();
        double absDiff = Math.abs(posDiff);
        if(absDiff > Math.PI){
            //the distance the other way around the circle
            posDiff = posDiff - (Constants.TWO_PI*Math.signum(posDiff));
        }

        // //This is for inverting the motor if target angle is 90-270 degrees away (not ready yet)
        // //To fix going the wrong way around the circle
        // if(absDiff >= Constants.THREE_PI_OVER_TWO){
        //     //the distance the other way around the circle
        //     posDiff = posDiff - (Constants.TWO_PI*Math.signum(posDiff));
        // //if between 90 and 270 invert the motor
        // }else if(absDiff < Constants.THREE_PI_OVER_TWO && absDiff > Constants.PI_OVER_TWO){
        //     //switch the motor inversion
        //     isInverted = !isInverted;
        //     //Since inverted, recompute everything
        //     posDiff = targetPos - getPosInRad();
        //     absDiff = Math.abs(posDiff);
        //     if(absDiff > Constants.THREE_PI_OVER_TWO){
        //         //the distance the other way around the circle
        //         posDiff = posDiff - (Constants.TWO_PI*Math.signum(posDiff));
        //     }
        // }
     
        //Convert the shortest distance to encoder value(use convertion factor) 
        double targetEncDistance = posDiff*Constants.RAD_TO_ENC_CONV_FACTOR;
        //add the encoder distance to the current encoder count
        double outputEncValue = targetEncDistance + rotationEncoder.getPosition();
        
        //Set the setpoint using setReference on the PIDController
        rotatePID.setReference(outputEncValue, ControlType.kPosition);
    }

    /**
     * this method is used to stop the module completely.
     */
    public void stopAll(){
        driveMotor.set(0.0);
        rotatePID.setReference(0.0,ControlType.kVoltage);
    }
}
