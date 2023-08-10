#!/bin/bash

# Directory to watch for new files
WATCH_DIR="/home/ec2-user/OpenSky/Data/"

# S3 bucket and path to upload to
S3_BUCKET="s3://ai-hackathon-event-bucket-2/"

# Function to handle file upload
upload_to_s3() {
    local file_path="$1"
    local file_name=$(basename "$file_path")
    echo "Processing file $file_name at $file_path"
    
    # Copy the file to S3
    aws s3 cp "$file_path" "$S3_BUCKET$file_name"

    # Optionally, delete the file from the instance store
    rm "$file_path"
}

# Handle existing files in the directory
for existing_file in "$WATCH_DIR"*; do
    if [ -f "$existing_file" ]; then
        upload_to_s3 "$existing_file"
    fi
done

# Loop forever, watching for files to be created in the watch directory
inotifywait -m "$WATCH_DIR" -e create |
    while read path action file; do
        upload_to_s3 "$path/$file"
    done