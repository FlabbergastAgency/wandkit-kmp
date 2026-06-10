// swift-tools-version:5.8
import PackageDescription

// BEGIN KMMBRIDGE VARIABLES BLOCK (do not edit)
let remoteKotlinUrl = "https://maven.pkg.github.com/FlabbergastAgency/wandkit-kmp/com/flabbergast/wandkit-core-kmmbridge/0.1.0/wandkit-core-kmmbridge-0.1.0.zip"
let remoteKotlinChecksum = "d03976cae6f7f317b6365610fc1d0cb81eb4cda0f285c00a7a09f5cc9e5099aa"
let packageName = "WandKitCore"
// END KMMBRIDGE BLOCK

let package = Package(
    name: packageName,
    platforms: [
        .iOS(.v14)
    ],
    products: [
        .library(
            name: packageName,
            targets: [packageName]
        ),
    ],
    targets: [
        .binaryTarget(
            name: packageName,
            url: remoteKotlinUrl,
            checksum: remoteKotlinChecksum
        )
        ,
    ]
)