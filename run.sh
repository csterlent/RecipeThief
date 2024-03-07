#!/bin/bash
javac -cp 'lwjgl.jar:../..' RecipeThief.java &&
java '-Djava.library.path=natives' -cp 'lwjgl.jar:../..' net.chase.RecipeThief
