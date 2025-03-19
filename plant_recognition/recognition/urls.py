from django.urls import path
from .views import identify_plant

urlpatterns = [
    path('identify-plant/', identify_plant, name='identify-plant'),
]
