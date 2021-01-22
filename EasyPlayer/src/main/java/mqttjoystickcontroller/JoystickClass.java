/**
 * This project is made by Vincenzo Petrolo on April 2020
 */
package mqttjoystickcontroller;

import android.widget.TextView;

import io.github.controlwear.virtual.joystick.android.JoystickView;
//import mqtt_controller.application.R;
import org.easydarwin.easyplayer.R;
import org.easydarwin.easyplayer.activity.PlayActivity;


public class JoystickClass {
    private TextView mTextViewAngleLeft;
    private TextView mTextViewStrengthLeft;
    private TextView mTextViewAngleRight;
    private TextView mTextViewStrengthRight;
    private TextView mTextViewCoordinateRight;


    private JoystickView leftJoystick;
    private JoystickView rightJoystick;


    /**
     * Initialize the controller class
     * @param controllerActivity
     */
    public JoystickClass(final PlayActivity controllerActivity){
        this.mTextViewAngleLeft     = (TextView) controllerActivity.findViewById(R.id.textView_angle_left);
        this.mTextViewStrengthLeft  = (TextView) controllerActivity.findViewById(R.id.textView_strength_left);
//        this.mTextViewAngleRight    = (TextView) controllerActivity.findViewById(R.id.textView_angle_right);
//        this.mTextViewStrengthRight = (TextView) controllerActivity.findViewById(R.id.textView_strength_right);
//        this.mTextViewCoordinateRight = (TextView) controllerActivity.findViewById(R.id.textView_coordinate_right);

        this.leftJoystick           = (JoystickView) controllerActivity.findViewById(R.id.joystickView_left);
//        this.rightJoystick          = (JoystickView) controllerActivity.findViewById(R.id.joystickView_right);

        final TextView tmpLeftAngleTextView        = this.mTextViewAngleLeft;
        final TextView tmpLeftStrenghtTextView     = this.mTextViewStrengthLeft;
//        final TextView tmpRightAngleTextView       = this.mTextViewAngleRight;
//        final TextView tmpRightStrenghtTextView    = this.mTextViewStrengthRight;

        this.leftJoystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                tmpLeftAngleTextView.setText(angle + "°");
                tmpLeftStrenghtTextView.setText(strength + "%");

                controllerActivity.getMqttManager().sendMessage(Integer.toString(angle) + " " + Integer.toString(strength),controllerActivity.getTopic()+"/right");

            }
        });

//        this.rightJoystick.setOnMoveListener(new JoystickView.OnMoveListener() {
//            @Override
//            public void onMove(int angle, int strength) {
//                tmpRightAngleTextView.setText(angle + "°");
//                tmpRightStrenghtTextView.setText(strength + "%");
//
//                controllerActivity.getMqttManager().sendMessage(Integer.toString(angle) + " " + Integer.toString(strength),controllerActivity.getTopic()+"/right");
//            }
//        });
    }
}
