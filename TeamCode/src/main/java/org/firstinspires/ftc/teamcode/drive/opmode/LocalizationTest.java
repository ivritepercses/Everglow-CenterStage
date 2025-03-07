package org.firstinspires.ftc.teamcode.drive.opmode;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.trajectory.Trajectory;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.DrivingSystem;
import org.firstinspires.ftc.teamcode.drive.SampleMecanumDrive;

/**
 * This is a simple teleop routine for testing localization. Drive the robot around like a normal
 * teleop routine and make sure the robot's estimated pose matches the robot's actual pose (slight
 * errors are not out of the ordinary, especially with sudden drive motions). The goal of this
 * exercise is to ascertain whether the localizer has been configured properly (note: the pure
 * encoder localizer heading may be significantly off if the track width has not been tuned).
 */
@TeleOp(group = "test")
public class LocalizationTest extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        DrivingSystem drive = new DrivingSystem(this);

        drive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        drive.setPoseEstimate(drive.PoseInTiles(1,5,0));

        waitForStart();

        while (!isStopRequested()) {
            if(gamepad1.circle){
                Trajectory trajectory = drive.trajectoryBuilder(new Pose2d())
                        .forward(100)
                        .build();
                drive.followTrajectory(trajectory);
            }
            drive.setWeightedDrivePower(
                    new Pose2d(
                            -gamepad1.left_stick_y,
                            -gamepad1.left_stick_x,
                            -gamepad1.right_stick_x
                    )
            );

            drive.update();

            Pose2d poseEstimate = drive.getPoseEstimate();
            telemetry.addData("x", poseEstimate.getX());
            telemetry.addData("y", poseEstimate.getY());
            telemetry.addData("heading", poseEstimate.getHeading());
            telemetry.addData("Tile X", poseEstimate.getX() / 60.5);
            telemetry.addData("Tile Y", poseEstimate.getY() / 60.5);

            telemetry.update();
        }
    }
}
