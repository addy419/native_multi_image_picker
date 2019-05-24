import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart' show Colors,Color;
import 'package:flutter/services.dart';

enum ReturnMode {
  NONE,
  ALL,
  CAMERA_ONLY,
  GALLERY_ONLY,
}

class NativeMultiImagePicker {
  static const MethodChannel _channel = const MethodChannel('image_picker');

  static Future<List<File>> start({
    ReturnMode returnMode = ReturnMode.NONE,
    bool folderMode = false,
    String toolbarFolderTitle = 'Folder',
    String toolbarImageTitle = 'Tap to Select',
    Color toolbarArrowColor = Colors.black,
    bool includeVideo = false,
    bool singleMode = false,
    int limit = 99,
    bool showCamera = true,
    String imageDirectory = 'Camera',
    String toolbarDoneButtonText = 'Done',
    bool enableLog = false,
  }) async {
    if (!Platform.isAndroid)
      throw new Exception('Only implemented for Android');
    else if (limit < 1)
      throw new Exception('Limit should be positive integer');
    else if (returnMode == ReturnMode.ALL ||
        returnMode == ReturnMode.GALLERY_ONLY) {
      if (!singleMode)
        throw new Exception('ReturnMode.ALL and ReturnMode.GALLERY_ONLY should only be used with singleMode');
    }
    List<dynamic> imageList;
    try {
      imageList = await _channel.invokeMethod(
        'startImagePicker',
        {
          'returnMode': returnMode.index + 1,
          'folderMode': folderMode,
          'toolbarFolderTitle': toolbarFolderTitle,
          'toolbarImageTitle': toolbarImageTitle,
          'toolbarArrowColor': toolbarArrowColor.value,
          'includeVideo': includeVideo,
          'singleMode': singleMode,
          'limit': limit,
          'showCamera': showCamera,
          'imageDirectory': imageDirectory,
          'toolbarDoneButtonText': toolbarDoneButtonText,
          'enableLog': enableLog,
        },
      );
    } on PlatformException catch(e) {
      throw new Exception(e.message);
    }
    if (imageList == null)
      return null;
    else {
      List<File> returnList = new List<File>();
      imageList.forEach((dynamic path){
        File file = new File(path);
        returnList.add(file);
      });
      return returnList;
    }
  }

  static Future<List<File>> captureCamera() async {
    return null;
  }
}
