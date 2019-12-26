package ch.epfl.esl.sportstracker;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

public class GameActivity extends AppCompatActivity {

    private ArFragment arFragment;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);

        // Executed when user taps on plane (detected by arFragment)
        arFragment.setOnTapArPlaneListener(((hitResult, plane, motionEvent) -> {
            // Describe fixed location and orientation in real world
            // 3D model displayed on top of anchor
            Anchor anchor = hitResult.createAnchor();

            // Build the model
            ModelRenderable.builder()
                    .setSource(this, Uri.parse("model.sfb"))
                    .build()
                    .thenAccept(modelRenderable -> addModelToScene(anchor, modelRenderable))
                    .exceptionally(throwable -> {
                        // If an error happens
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage(throwable.getMessage())
                                .show();
                        return null;
                    });
        }));

    }

    private void addModelToScene(Anchor anchor, ModelRenderable modelRenderable) {
        // Automatically positions itself on given anchor (cannot be changed)
        AnchorNode anchorNode = new AnchorNode(anchor);

        // Node that can be changed (position or zoom)
        TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
        // Place it where anchorNode is
        transformableNode.setParent(anchorNode);
        // Give it the model
        transformableNode.setRenderable(modelRenderable);

        // Add to scene
        arFragment.getArSceneView().getScene().addChild(anchorNode);

        // Select transformableNode
        transformableNode.select();
    }
}
