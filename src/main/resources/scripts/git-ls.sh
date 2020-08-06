#!/usr/bin/env bash

# script: http://mywiki.wooledge.org/BashFAQ/035
die() {
  printf '%s\n' "$1" >&2
  exit 1
}

# Initialize all the option variables.
# This ensures we are not contaminated by variables from the environment.

to=
from=

while :; do
  case $1 in
  -t=?*)
    to="${1#*=}"
    ;;
  -t=)
    die 'ERROR: "-t" requires a non-empty option argument.'
    ;;
  -f=?*)
    from="${1#*=}"
    ;;
  *) # Default case: No more options, so break out of the loop.
    break ;;
  esac
  shift
done

git log --pretty='{subject: "%s"}' "${to}".."${from}"
