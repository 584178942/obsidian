#!/bin/bash
cd /root/.openclaw/obsidian
echo "Pushing to GitLab..."
git push origin master
echo "Pushing to GitHub..."
git push github master
echo "Done!"
