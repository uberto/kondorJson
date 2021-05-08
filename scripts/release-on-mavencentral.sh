# Steps:
# remove snapshot from the version manually in build.gradle
# update the README.md
# check the CHANGELOG.md
# verify it's all working with a ./gradlew clean build
# launch:

./gradlew uploadArchives -p kondor-core
./gradlew uploadArchives -p kondor-tools
./gradlew uploadArchives -p kondor-outcome

# then go to sonatype site and login
# https://oss.sonatype.org/#nexus-search;quick~kondor
# select Staging Repositories, and close the corresponding one (empty desc is fine)
# then click release and wait ~10 min to be able to download it
# and then bouncing the version with SNAPSHOT in build.gradle
# commit new shapshot version
