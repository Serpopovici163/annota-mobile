import os, io
from google.cloud import vision

os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = r'googleAPI_key.json'
client = vision.ImageAnnotatorClient()

FOLDER_PATH = r'C:\Users\Trololololol\Documents\uOttawa\Summer 2021\GNG2101\GNG2101_Project\Server'
IMAGE_FILE = 'messy_writing.jpg'
FILE_PATH = os.path.join(FOLDER_PATH, IMAGE_FILE)

with io.open(FILE_PATH, 'rb') as image_file:
    content = image_file.read()

image = vision.Image(content=content)
response = client.document_text_detection(image=image)

print(response.full_text_annotation.text)