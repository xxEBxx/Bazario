package com.example.New_bazario.controllers;

import com.example.New_bazario.entities.Category;
import com.example.New_bazario.entities.Product;
import com.example.New_bazario.services.CategoryService;
import com.example.New_bazario.services.ProductService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductViewController {
    private final ProductService productService;
    private final CategoryService categoryService;

    @Autowired
    public ProductViewController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    // Product listing endpoint
    @GetMapping
    public String listProducts(
            @RequestParam(required = false, defaultValue = "") List<Integer> categoryIds,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minStock,
            Model model) {

        try {
            // Ensure categoryIds is never null
            if (categoryIds == null) {
                categoryIds = new ArrayList<>();
            }

            // Fetch categories for UI
            List<Category> categories = categoryService.getAllCategories();
            model.addAttribute("categories", categories);

            // Fetch products with filters
            List<Product> products;
            if (!categoryIds.isEmpty()) {
                products = productService.getProductsByCategories(categoryIds);
            } else {
                products = productService.advancedFilterProducts(
                    categoryIds, minPrice, maxPrice, minStock, searchTerm);
            }

            // Add everything to model
            model.addAttribute("products", products != null ? products : new ArrayList<>());
            model.addAttribute("selectedCategories", categoryIds);
            model.addAttribute("searchTerm", searchTerm);
            model.addAttribute("minPrice", minPrice);
            model.addAttribute("maxPrice", maxPrice);
            model.addAttribute("minStock", minStock);

            return "products";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "An error occurred while fetching products");
            model.addAttribute("products", new ArrayList<>());
            return "products";
        }
    }

    // Product details endpoint
    @GetMapping("/{productId}")
    public String getProductDetails(@PathVariable Integer productId, Model model) {
        try {
            Product product = productService.getProductById(productId);
            Category category = categoryService.getCategoryById(product.getCategoryId());
            
            // Add these debug prints
            System.out.println("Product ID: " + product.getProductId());
            System.out.println("Category: " + category.getName());
            
            model.addAttribute("product", product);
            model.addAttribute("category", category);
            return "product-details";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/error";
        }
    }
}