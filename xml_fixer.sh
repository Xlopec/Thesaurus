#!/usr/bin/env bash
for filename in ~/IdeaProjects/papers/unzipped/*.fb2; do
    xmlstarlet fo -o -R -D "$filename" > "fixed/$(basename $filename)"
done