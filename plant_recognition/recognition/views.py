import os
import traceback
import requests
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.core.files.storage import default_storage
from django.core.files.base import ContentFile

PLANTNET_API_KEY = "2b10r2y9pGKDug2UXEqv9P4I3u"
PLANTNET_URL = "https://my-api.plantnet.org/v2/identify"

@csrf_exempt
def identify_plant(request):
    try:
        if request.method == "POST" and request.FILES.get("image"):
            image_file = request.FILES["image"]
            
            # ✅ Save the file in a temp directory
            temp_dir = "temp/"
            os.makedirs(temp_dir, exist_ok=True)  # Ensure the temp folder exists
            temp_file_path = os.path.join(temp_dir, image_file.name)

            with open(temp_file_path, "wb") as f:
                for chunk in image_file.chunks():
                    f.write(chunk)

            try:
                # ✅ Open file in read-binary mode
                with open(temp_file_path, "rb") as img:
                    files = {"images": img}
                    params = {"api-key": PLANTNET_API_KEY}
                    response = requests.post(PLANTNET_URL, files=files, params=params)

                # ✅ Delete the file safely after sending request
                os.remove(temp_file_path)

                if response.status_code == 200:
                    data = response.json()
                    plant_name = data.get("results", [{}])[0].get("species", {}).get("scientificNameWithoutAuthor", "Unknown")
                    return JsonResponse({"plant_name": plant_name})

                return JsonResponse({"error": f"Failed to identify plant: {response.text}"}, status=500)

            except Exception as e:
                traceback.print_exc()
                return JsonResponse({"error": f"Internal error: {str(e)}"}, status=500)

    except Exception as e:
        traceback.print_exc()
        return JsonResponse({"error": f"Unexpected error: {str(e)}"}, status=500)

    return JsonResponse({"error": "Invalid request"}, status=400)
