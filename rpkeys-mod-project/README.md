# RP Ability Keybinds + SVM Powers (one combined jar)
`./gradlew build` compiles the client mod (radial wheel + keybinds). `merge_jar.py` then merges it into `base/svm-powers-rp.jar`
(the datapack/mod jar) to make `build/combined/svm-powers-combined.jar` - the only file players need.
To change the datapack: replace `base/svm-powers-rp.jar` with the new jar and push; GitHub rebuilds the combined jar.
