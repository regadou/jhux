package org.regadou.jhux.ns;

public class GeoLocation {

   private double longitude;
   private double latitude;
   private double altitude;
   private String txt;
   
   public GeoLocation() {}
   
   public GeoLocation(double latitude, double longitude, double altitude) {
      this.latitude = latitude;
      this.longitude = longitude;
      this.altitude = altitude;
   }

   public GeoLocation(String name) {
      if (name.startsWith("geo:")) {
         String[] parts = name.substring(4).split(";")[0].split(",");
         latitude = Double.parseDouble(parts[0]);
         longitude = Double.parseDouble(parts[1]);
         altitude = Double.parseDouble(parts[2]);
         //TODO: we need to add measure precision
      }
      throw new RuntimeException("Geolocation of names not implemented yet");
      //TODO: use a geo location service to locate the object referred by this name (address, city, region, lake, river, mountain, ...)
   }

   public double getLatitude() {
      return latitude;
   }

   public double getLongitude() {
      return longitude;
   }

   public double getAltitude() {
      return altitude;
   }

   @Override
   public String toString() {
      if (txt == null)
         txt = "geo:"+latitude+","+longitude+","+altitude;
      return txt;
   }
}
