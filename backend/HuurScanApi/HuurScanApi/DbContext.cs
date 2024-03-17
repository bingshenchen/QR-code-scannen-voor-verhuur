using HuurScanApi.Model;
using Microsoft.EntityFrameworkCore;

namespace HuurScanApi
{
    public class RentalContext : DbContext
    {
        public RentalContext(DbContextOptions<RentalContext> options)
            : base(options)
        {
        }

        public DbSet<Rental> Rentals { get; set; }
    }
}
