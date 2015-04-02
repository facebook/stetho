#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DUMPAPP="$DIR/dumpapp"

set -e

# This will generate an hprof on the device, download it locally, convert the
# hprof to the standard format, and store it in the current working directory.
# The resulting file can be explored with a tool such as the standalone Eclipse
# MemoryAnalyzer: https://eclipse.org/mat/

if [[ -z "$1" ]]; then
  OUTFILE="out.hprof"
else
  OUTFILE=$1
fi
TEMPFILE="${OUTFILE}-dalvik.tmp"

echo "Generating hprof on device (this can take a while)..."
$DUMPAPP "$@" hprof - > ${TEMPFILE}

echo "Converting $TEMPFILE to standard format..."
hprof-conv $TEMPFILE $OUTFILE
rm $TEMPFILE

echo "Stored ${OUTFILE}"
