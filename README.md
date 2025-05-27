# Cloth Authentication System

A Java-based system for authenticating cloth items using computer vision and feature extraction techniques. This system helps identify and verify the authenticity of textile products by analyzing their unique visual characteristics.

## Features

- **Deep Feature Extraction**: Analyzes multiple aspects of cloth items:
  - Fabric texture analysis
  - Color histogram
  - Pattern complexity and symmetry
  - Edge features
  - Dimensional measurements

- **Secure Authentication**: 
  - SHA-256 hashing for feature storage
  - Robust comparison algorithm with tolerance for lighting variations
  - Multi-factor authentication using various cloth characteristics

- **Local Storage**:
  - Secure storage of cloth features and identities
  - JSON-based data persistence
  - Organized storage structure for features and identities

## System Requirements

- Java JDK 11 or higher
- Maven for dependency management
- OpenCV library (automatically included in dependencies)

## Installation

1. Clone the repository:
```powershell
git clone [repository-url]
cd cloth-authentication-system
```

2. Build the project using Maven:
```powershell
mvn clean package
```

3. Run the application:
```powershell
java -jar target/cloth-authentication-system-1.0.0-jar-with-dependencies.jar
```

## Usage

The system provides a command-line interface with the following options:

1. **Process New Cloth Item**
   - Captures and stores cloth features
   - Generates unique cloth ID
   - Creates digital identity with secure hashes

2. **Verify Existing Cloth**
   - Verifies authenticity using cloth ID
   - Compares features with stored data
   - Shows detailed similarity scores

3. **List Stored Cloths**
   - Displays all registered cloth items
   - Shows creation dates and IDs

4. **Delete Cloth Data**
   - Removes cloth records from storage

## Technical Details

### Feature Extraction
- Texture Analysis:
  - Mean intensity
  - Contrast
  - Standard deviation
  - Homogeneity

- Pattern Analysis:
  - Complexity score
  - Symmetry score
  - Edge detection

- Dimensional Analysis:
  - Area calculation
  - Aspect ratio
  - Width and height measurements

### Authentication Process
1. Feature extraction from input image
2. Normalization of features
3. Generation of secure hashes
4. Comparison with stored features using weighted scoring:
   - Texture features (40%)
   - Pattern features (40%)
   - Dimensional features (20%)
5. Authentication threshold of 80% similarity

### Data Storage
- Features stored in: `data/features/`
- Identities stored in: `data/identities/`
- Images stored in: `data/images/`

## Project Structure

```
cloth-authentication-system/
├── src/main/java/com/clothauth/
│   ├── ClothAuthenticationApp.java
│   ├── extractor/
│   │   └── ClothFeatureExtractor.java
│   ├── model/
│   │   ├── ClothFeatures.java
│   │   └── ClothIdentity.java
│   ├── security/
│   │   └── HashGenerator.java
│   ├── storage/
│   │   └── LocalStorageManager.java
│   └── utils/
│       └── ImageProcessor.java
├── data/
│   ├── features/
│   ├── identities/
│   └── images/
└── pom.xml
```

## Security Features

- Normalized feature hashing for consistent comparison
- Timestamp-based versioning
- Secure hash generation using SHA-256
- Feature comparison with tolerance for environmental variations

## Best Practices for Use

1. Ensure consistent lighting conditions during image capture
2. Position cloth items flat and wrinkle-free
3. Capture the entire cloth item in the image
4. Maintain consistent camera angle and distance
5. Use high-resolution images for better feature extraction

## Contributing

Contributions are welcome. Please follow these steps:
1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

[MIT License](LICENSE)

## Contact

For questions and support, please open an issue in the GitHub repository. 
