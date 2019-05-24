import 'package:flutter/material.dart';
import 'dart:async';
import 'dart:io';

import 'package:native_multi_image_picker/native_multi_image_picker.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  List<File> imageFiles;

  @override
  void initState() {
    imageFiles = null;
    super.initState();
  }

  Future<void> openImagePicker() async {
    try {
      List<File> images = await NativeMultiImagePicker.start();
      if (images == null)
        print('Operation cancelled');
      else
        setState(() {
          imageFiles = images;
        });
    } catch (e) {
      print(e);
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: imageFiles != null
              ? ListView(
                  children: 
                    imageFiles.map((File f) => Image.file(f)).toList(),
                )
              : Container(),
        ),
        floatingActionButton: FloatingActionButton(
          child: Icon(Icons.photo),
          onPressed: () => openImagePicker(),
        ),
      ),
    );
  }
}
