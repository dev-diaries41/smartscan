# SmartScan - Image organisation & search app

SmartScan is an innovative app powered by a CLIP model that automatically organizes your images by content similarity and enables text-based search, making gallery management effortless.


<div align="center">
  <img src="fastlane/metadata/android/en-US/images/featureGraphic.png" alt="Banner" style="border-radius: 20px;">
</div>


| <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" alt="Screenshot 1" width="200px"> | <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" alt="Screenshot 2" width="200px"> | <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" alt="Screenshot 3" width="200px"> |
| --- | --- | --- |
| <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" alt="Screenshot 4" width="200px"> | <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" alt="Screenshot 5" width="200px"> | <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" alt="Screenshot 6" width="200px"> |

---


## Key Features

### Automated Image Organisation
- **Smart Scans:** Schedule regular scans automatically.
- **Auto-Categorisation Test:** Upload a test image to verify categorisation.

### Text-Based Image Search
- **Gallery Access:** Open search results directly in the gallery.
- **Result Display:** Show the top result plus up to 20 similar images.
- **Weekly Updates:** Automatically refresh the image index for new images.

### Fully On-Device
All processing—including both image organisation and text-based search—is handled entirely on-device, ensuring privacy, speed, and offline functionality.

---

## How to use


### Image Management

1. **Target Folders**: In settings, select the folders you want to monitor for new images.
2. **Destination Folders**: In settings, select the allowed destination folders for images to be moved to.
3. **Test auto-categorisation**: Once you have set this up you can test auto-categorisation by clicking the `test tube` icon and uploading an image. 

**Note:** Each destination folder should contain at least 10 images. This sample set is crucial for accurate matching.

<div style="display: flex; justify-content: space-between; padding-top: 20px;">
  <img src="fastlane/metadata/android/en-US/images/other/target.png" alt="Target folders" style="border-radius: 15px; margin: 5px; width:200px;">
  <img src="fastlane/metadata/android/en-US/images/other/destination.png" alt="Destination folders" style="border-radius: 15px; margin: 5px; width:200px;">
</div>


#### Limitations

- The accuracy of auto-categorization depends on the consistency of the sample images in each destination folder.
- False moves may occur if the images in a destination folder do not accurately represent the intended content.
- To minimize inaccuracies, ensure your destination folders are well-curated. If issues persist, **disable scanning** and **open a GitHub issue**. Repeated issues might result in the removal of this feature.

### Image Search

- **Indexing Requirement**: The first time you use the app, images must be indexed. Depending on your gallery size, this may take a few minutes.
- **Ongoing Updates**: Image indexing is updated on a weekly schedule to include new images.

#### Search Tips

- **Keep it Short**: Use short, specific search queries.
- **Be Descriptive**: For images with text content, include specific words present in the image.
- **Adjust Results**: In settings, you can modify the number of similar results displayed.


## Download

Go to [Releases](https://github.com/dev-diaries41/smartscan/releases/latest) and download the latest apk.

<div style="display: flex; gap: 10px;">
  <a href="https://f-droid.org/packages/com.fpf.smartscan" style="text-decoration: none;">
  <img src="https://f-droid.org/badge/get-it-on.svg" alt="F-Droid" style="max-width:100%;" width=200>
  </a>
</div>


## Buy Me A Coffee

The app is free, but if you enjoy using it and want to support project development and maintenance, please consider donating using one of the options below:

| Wallet   | Address                              |
| -------- | ------------------------------------ |
| Bitcoin  | bc1qw46nxjp5gkh460ewamjd3jfeu0xv6ytq5et6xm        |
| Ethereum | 0xa53aC18B25942C71019f85314AF67F6132E525ad       |
| Litecoin | ltc1q2hspfea9rw5j2ymvv22hx7dmckh8c99sqk7av3       |

---

## License

 * This project is licensed under the GNU General Public License, Version 3 (GPLv3).
 * See the LICENSE file for details.

---
