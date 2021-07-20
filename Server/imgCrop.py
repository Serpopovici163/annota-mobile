from PIL import Image
import os

image = Image.open(os.path.join(os.getcwd(), "image.jpg"))
#drawing = Image.open(os.path.join(os.getcwd(), "drawing.jpg"))
#image.paste(drawing, (0,0))
width, height = image.size

#from here we can make a square assuming that height > width
top = (height - width) / 2
bottom = top + width

image = image.crop((0, top, width, bottom))
image = image.resize((80,80), Image.ANTIALIAS)
image.save(os.path.join(os.getcwd(), "icon.png"))