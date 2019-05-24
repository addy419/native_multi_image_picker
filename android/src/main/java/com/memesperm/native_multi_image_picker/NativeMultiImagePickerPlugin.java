package com.memesperm.native_multi_image_picker;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.provider.CalendarContract;
import android.widget.ImageView;
import android.os.Environment;

import java.util.ArrayList;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.features.ImagePickerConfig;
import com.esafirm.imagepicker.features.IpCons;
import com.esafirm.imagepicker.features.ReturnMode;
import com.esafirm.imagepicker.features.imageloader.ImageLoader;
import com.esafirm.imagepicker.model.Image;
import com.esafirm.imagepicker.features.imageloader.ImageType;

/** ImagePickerPlugin */
public class NativeMultiImagePickerPlugin implements MethodCallHandler,PluginRegistry.ActivityResultListener {
  private static final String CHANNEL_NAME = "image_picker";
  private static final int REQUEST_CODE = IpCons.RC_IMAGE_PICKER;

  private final Activity activity;
  private final Context context;
  private Result pendingResult;
  private MethodCall methodCall;

  private NativeMultiImagePickerPlugin(Activity activity, Context context) {
    this.activity = activity;
    this.context = context;
  }
  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), CHANNEL_NAME);
    NativeMultiImagePickerPlugin instance = new NativeMultiImagePickerPlugin(registrar.activity(), registrar.context());
    registrar.addActivityResultListener(instance);
    channel.setMethodCallHandler(instance);
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (!setPendingMethodCallAndResult(call, result)) {
      finishWithAlreadyActiveError(result);
      return;
    }

    if (call.method.equals("startImagePicker"))
      this.start();
    else if(call.method.equals("startCamera"))
      this.captureImage();
    else {
      result.notImplemented();
      clearMethodCallAndResult();
    }
  }

  private void start() {
    try {
      ImagePicker imagePicker = getImagePicker();
      Intent intent = imagePicker.getIntent(context);
      ImagePickerConfig config = imagePicker.getConfig();
      if (!activity.isFinishing())
        activity.startActivityForResult(intent, REQUEST_CODE);
    } catch(Exception e) {
      finishWithError();
    }
  }

  private void captureImage() {
    try {
      Intent intent = ImagePicker.cameraOnly().getIntent(context);
      if (!activity.isFinishing())
        activity.startActivityForResult(intent, REQUEST_CODE);
    } catch(Exception e) {
      finishWithError();
    }
  }

  private ImagePicker getImagePicker() {
    Integer retMode = methodCall.argument("returnMode");
    Boolean folderMode = methodCall.argument("folderMode");
    String toolbarFolderTitle = methodCall.argument("toolbarFolderTitle");
    String toolbarImageTitle = methodCall.argument("toolbarImageTitle");
    Long toolbarArrowColor = methodCall.argument("toolbarArrowColor");
    Boolean includeVideo = methodCall.argument("includeVideo");
    Boolean singleMode = methodCall.argument("singleMode");
    Integer limit = methodCall.argument("limit");
    Boolean showCamera = methodCall.argument("showCamera");
    String imageDirectory = methodCall.argument("imageDirectory");
    String toolbarDoneButtonText = methodCall.argument("toolbarDoneButtonText");
    Boolean enableLog = methodCall.argument("enableLog");

    ReturnMode returnMode;
    switch(retMode) {
      case 1: returnMode = ReturnMode.NONE;
        break;
      case 2: returnMode = ReturnMode.ALL;
        break;
      case 3: returnMode = ReturnMode.CAMERA_ONLY;
        break;
      case 4: returnMode = ReturnMode.GALLERY_ONLY;
        break;
      default: returnMode = ReturnMode.ALL;
    }

    ImagePicker imagePicker = ImagePicker.create(activity)
            .language("en") // Set image picker language
            .theme(R.style.ImagePickerTheme)
            .returnMode(returnMode) // set whether pick action or camera action should return immediate result or not. Only works in single mode for image picker
            .folderMode(folderMode)
            .toolbarFolderTitle(toolbarFolderTitle)
            .toolbarImageTitle(toolbarImageTitle)
            .toolbarArrowColor(toolbarArrowColor.intValue())
            .includeVideo(includeVideo) // include video (false by default)
            .limit(limit)
            .showCamera(showCamera)
            .imageDirectory(imageDirectory)
            .includeAnimation(true)
            .enableLog(enableLog)
            .toolbarDoneButtonText(toolbarDoneButtonText)
            .imageLoader(new GlideImageLoader());

    if (singleMode) {
      imagePicker.single();
    } else {
      imagePicker.multi(); // multi mode (default mode)
    }

    return imagePicker.imageFullDirectory(Environment.getExternalStorageDirectory().getPath()); // can be full path
  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, Intent data){
    if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
      ArrayList<Image> images;
      images = (ArrayList<Image>) ImagePicker.getImages(data);
      ArrayList<String> paths = new ArrayList<String>();
      for(Image i : images) {
        paths.add(i.getPath());
      }
      finishWithSuccess(paths);
      return true;
    }
    else if(requestCode == REQUEST_CODE) {
      finishWithSuccess(null);
      return true;
    }
    finishWithError();
    return false;
  }

  private void finishWithSuccess(ArrayList<String> returnList) {
    pendingResult.success(returnList);
    clearMethodCallAndResult();
  }

  private void finishWithError() {
    pendingResult.error("cannot_fetch","error fetching images from image_picker",null);
    clearMethodCallAndResult();
  }

  private void finishWithAlreadyActiveError(MethodChannel.Result result) {
    if (result != null)
      result.error("already_active", "Image picker is already active", null);
  }

  private void clearMethodCallAndResult() {
    methodCall = null;
    pendingResult = null;
  }

  private boolean setPendingMethodCallAndResult(MethodCall methodCall, MethodChannel.Result result) {
    if (pendingResult != null) {
      return false;
    }

    this.methodCall = methodCall;
    pendingResult = result;
    return true;
  }
}

class GlideImageLoader implements ImageLoader{

  @Override
  public void loadImage(String path,ImageView imageView,ImageType imageType){
    int placeholderResId = imageType == imageType.FOLDER ? R.drawable.ef_folder_placeholder : R.drawable.ef_image_placeholder;

    GlideApp.with(imageView.getContext())
            .load("file://"+path)
            .centerCrop()
            .placeholder(placeholderResId)
            .error(placeholderResId)
            .into(imageView);
  }
}
