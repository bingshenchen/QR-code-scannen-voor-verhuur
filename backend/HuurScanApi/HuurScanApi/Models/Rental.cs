namespace HuurScanApi.Model
{
    public class Rental
    {
        public int Id { get; set; }
        public String? Name { get; set; }
        public bool IsRented { get; set; }

        private DateTime? rentedDate;
        public DateTime? RentedDate
        {
            get { return rentedDate; }
            set { rentedDate = IsRented ? value : null; }
        }
        
    }
}
