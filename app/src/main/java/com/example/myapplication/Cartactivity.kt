package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class CartActivity : AppCompatActivity() {

    private lateinit var tvTotal: TextView
    private lateinit var tvSubtotal: TextView
    private lateinit var listView: ListView
    private lateinit var adapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activitycart)

        tvTotal    = findViewById(R.id.tvCartTotal)
        tvSubtotal = findViewById(R.id.tvCartSubtotal)
        listView   = findViewById(R.id.lvCartItems)

        // ✅ Use new CartAdapter with edit (qty) and delete buttons
        adapter = CartAdapter(this, CartManager.cartItems) {
            updateTotals()
        }
        listView.adapter = adapter

        updateTotals()

        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener { finish() }

        findViewById<Button>(R.id.btnCheckout).setOnClickListener {
            if (CartManager.cartItems.isEmpty()) {
                Toast.makeText(this, "Your cart is empty!", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, CheckoutActivity::class.java).apply {
                    putParcelableArrayListExtra("SELECTED_PRODUCTS", ArrayList(CartManager.cartList))
                }
                startActivity(intent)
            }
        }
    }

    private fun updateTotals() {
        tvTotal.text    = "₱ ${"%.2f".format(CartManager.getTotalPrice())}"
        tvSubtotal.text = "${CartManager.getTotalItems()} Items"
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
        updateTotals()
    }

    // ✅ NEW: Cart adapter with + / - quantity controls and 🗑 delete per item
    class CartAdapter(
        private val context: Context,
        private val items: MutableList<CartItem>,
        private val onChanged: () -> Unit
    ) : BaseAdapter() {

        private val inflater = LayoutInflater.from(context)

        override fun getCount()        = items.size
        override fun getItem(i: Int)   = items[i]
        override fun getItemId(i: Int) = i.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(R.layout.item_cart, parent, false)
            val cartItem = getItem(position)
            val product  = cartItem.product

            view.findViewById<TextView>(R.id.tvCartItemName).text   = product.name
            view.findViewById<TextView>(R.id.tvCartItemSeller).text = "Sold by: ${product.seller}"
            view.findViewById<TextView>(R.id.tvCartItemPrice).text  =
                "₱ ${"%.2f".format(product.price * cartItem.quantity)}"
            view.findViewById<TextView>(R.id.tvCartQty).text        = cartItem.quantity.toString()

            // ✅ INCREASE quantity
            view.findViewById<Button>(R.id.btnIncrease).setOnClickListener {
                if (cartItem.quantity >= product.stock) {
                    Toast.makeText(context, "Max stock reached (${product.stock})", Toast.LENGTH_SHORT).show()
                } else {
                    CartManager.increaseQty(context, product.firebaseKey)
                    notifyDataSetChanged()
                    onChanged()
                }
            }

            // ✅ DECREASE quantity (removes item if qty reaches 0)
            view.findViewById<Button>(R.id.btnDecrease).setOnClickListener {
                CartManager.decreaseQty(context, product.firebaseKey)
                notifyDataSetChanged()
                onChanged()
            }

            // ✅ DELETE item with confirmation dialog
            view.findViewById<ImageView>(R.id.btnDeleteCartItem).setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Remove Item")
                    .setMessage("Remove \"${product.name}\" from your cart?")
                    .setPositiveButton("Remove") { _, _ ->
                        CartManager.removeFromCart(context, product)
                        notifyDataSetChanged()
                        onChanged()
                        Toast.makeText(context, "\"${product.name}\" removed", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            return view
        }
    }
}