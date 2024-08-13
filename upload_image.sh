function build_artifacts() {
    mvn clean package -DskipTests=true
}
VERSION=$1

if [ "$2" = prod ]; then
        ENV=stable-glass-183220
elif [ "$2" = dev ]; then
        ENV=q6dev-186015
else
        echo "Please specify either prod/dev"
        exit 1
fi
build_artifacts
NAME=metrics-accumulator
docker build -t q6cyber/$NAME:"$VERSION" .
docker tag q6cyber/$NAME:"$VERSION" us-docker.pkg.dev/$ENV/us.gcr.io/$NAME:"$VERSION"
docker push us-docker.pkg.dev/$ENV/us.gcr.io/$NAME:"$VERSION"
gcloud artifacts docker images list us-docker.pkg.dev/$ENV/us.gcr.io/$NAME
#gcloud artifacts repositories describe us.gcr.io \
#    --project=$ENV \
#    --location=us

#gcloud auth configure-docker us-docker.pkg.dev