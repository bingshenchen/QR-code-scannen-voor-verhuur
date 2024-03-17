using HuurScanApi.Model;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace HuurScanApi.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class RentalsController : Controller
    {

        private readonly RentalContext _context;

        public RentalsController(RentalContext context)
        {
            _context = context;
        }

        [HttpGet("{id}")]
        public async Task<ActionResult<Rental>> GetRentalStatus(int id)
        {
            var rental = await _context.Rentals.FindAsync(id);

            if (rental == null)
            {
                return NotFound();
            }

            return rental;
        }
        [HttpGet]
        public async Task<ActionResult<IEnumerable<Rental>>> GetAllRentals()
        {
            return await _context.Rentals.ToListAsync();
        }

        [HttpPost("{id}/rent")]
        public async Task<IActionResult> Rent(int id)
        {
            var rental = await _context.Rentals.FindAsync(id);
            if (rental == null || rental.IsRented)
            {
                return BadRequest("Item is either not found or already rented.");
            }

            rental.IsRented = true;
            await _context.SaveChangesAsync();

            return NoContent();
        }

        [HttpDelete("{id}")]
        public async Task<IActionResult> DeleteRental(int id)
        {
            var rental = await _context.Rentals.FindAsync(id);
            if (rental == null)
            {
                return NotFound();
            }

            _context.Rentals.Remove(rental);
            await _context.SaveChangesAsync();

            return NoContent();
        }


        [HttpPut("{id}")]
        public async Task<IActionResult> UpdateRental(int id, [FromBody] Rental rentalUpdate)
        {
            if (id != rentalUpdate.Id)
            {
                return BadRequest("The ID in the URL does not match the ID of the rental update data.");
            }

            var rental = await _context.Rentals.FindAsync(id);
            if (rental == null)
            {
                return NotFound();
            }

   
            if (!string.IsNullOrEmpty(rentalUpdate.Name))
            {
                rental.Name = rentalUpdate.Name;
            }

            rental.IsRented = rentalUpdate.IsRented;
            rental.RentedDate = rentalUpdate.IsRented ? DateTime.UtcNow : null;

            try
            {
                await _context.SaveChangesAsync();
            }
            catch (DbUpdateConcurrencyException)
            {
                if (!_context.Rentals.Any(e => e.Id == id))
                {
                    return NotFound();
                }
                else
                {
                    throw;
                }
            }

            return NoContent();
        }

        [HttpPost]
        public async Task<ActionResult<Rental>> AddRental(Rental rental)
        {
            var existingRental = await _context.Rentals.FindAsync(rental.Id);
            if (existingRental == null)
            {
                _context.Rentals.Add(rental);
                await _context.SaveChangesAsync();
                return CreatedAtAction(nameof(GetRentalStatus), new { id = rental.Id }, rental);
            }
            else
            {
                return BadRequest("Rental with the same ID already exists.");
            }
        }
    }
}
