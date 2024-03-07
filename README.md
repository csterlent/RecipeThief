Recipe browser for ReIndev (the Minecraft mod).

Includes an HTML page that displays all the recipes. The images in this page are encoded in base64. All the crafting and item info is in this page, copied from output.js.

Also includes a Java program that launches ReIndev and reads out important data into output.js. This is also where the images are base64 encoded.

I unzipped the ReIndev jar and created these files in a new directory net/chase. In net/chase I also included lwjgl.jar, as well as the lwjgl native binaries in a directory called natives. To allow Minecraft to populate all the important data, I added a 4 second delay. This might be far long enough already, or you might have to modify this.

It may have been easier for me to make a FoxLoader mod, but I haven't checked out FoxLoader...yet.

The program may also provide any recipes that were modded into ReIndev. The program can output item names from a different translation than en (US). The program can output different item textures if you find and modify the texture base path.
