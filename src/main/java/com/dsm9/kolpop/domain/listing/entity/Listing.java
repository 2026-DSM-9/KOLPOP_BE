package com.dsm9.kolpop.domain.listing.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.dsm9.kolpop.domain.user.entity.User;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "listings")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    @Column(nullable = false, length = 120)
    private String title;

    @ElementCollection
    @CollectionTable(name = "listing_images", joinColumns = @JoinColumn(name = "listing_id"))
    @OrderColumn(name = "image_order")
    @Column(name = "image_url", nullable = false, length = 500)
    private List<String> imageUrls = new ArrayList<>();

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "detail_address", length = 255)
    private String detailAddress;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "daily_fee", nullable = false)
    private Long dailyFee;

    @Column(nullable = false)
    private Long deposit;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal area;

    @ElementCollection
    @CollectionTable(name = "listing_facilities", joinColumns = @JoinColumn(name = "listing_id"))
    @OrderColumn(name = "facility_order")
    @Column(name = "facility_name", nullable = false, length = 50)
    private List<String> facilities = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "listing_industry_restrictions", joinColumns = @JoinColumn(name = "listing_id"))
    @OrderColumn(name = "restriction_order")
    @Column(name = "restriction_name", nullable = false, length = 80)
    private List<String> industryRestrictions = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "listing_additional_restrictions", joinColumns = @JoinColumn(name = "listing_id"))
    @OrderColumn(name = "additional_restriction_order")
    @Column(name = "restriction_name", nullable = false, length = 80)
    private List<String> additionalRestrictions = new ArrayList<>();

    @Column(name = "operating_start_date", nullable = false)
    private LocalDate operatingStartDate;

    @Column(name = "operating_end_date", nullable = false)
    private LocalDate operatingEndDate;

    @Column(name = "min_operating_days", nullable = false)
    private Integer minOperatingDays;

    @Column(name = "max_operating_days", nullable = false)
    private Integer maxOperatingDays;

    @Column(nullable = false, length = 500)
    private String description;

    @ElementCollection
    @CollectionTable(name = "listing_hashtags", joinColumns = @JoinColumn(name = "listing_id"))
    @OrderColumn(name = "hashtag_order")
    @Column(name = "hashtag", nullable = false, length = 50)
    private List<String> hashtags = new ArrayList<>();

    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Listing() {
    }

    public Listing(
            User landlord,
            String title,
            List<String> imageUrls,
            String address,
            String detailAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            Long dailyFee,
            Long deposit,
            BigDecimal area,
            List<String> facilities,
            List<String> industryRestrictions,
            List<String> additionalRestrictions,
            LocalDate operatingStartDate,
            LocalDate operatingEndDate,
            Integer minOperatingDays,
            Integer maxOperatingDays,
            String description,
            List<String> hashtags,
            LocalDateTime createdAt
    ) {
        this.landlord = landlord;
        this.title = title;
        this.imageUrls = new ArrayList<>(imageUrls);
        this.address = address;
        this.detailAddress = detailAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.dailyFee = dailyFee;
        this.deposit = deposit;
        this.area = area;
        this.facilities = new ArrayList<>(facilities);
        this.industryRestrictions = new ArrayList<>(industryRestrictions);
        this.additionalRestrictions = new ArrayList<>(additionalRestrictions);
        this.operatingStartDate = operatingStartDate;
        this.operatingEndDate = operatingEndDate;
        this.minOperatingDays = minOperatingDays;
        this.maxOperatingDays = maxOperatingDays;
        this.description = description;
        this.hashtags = new ArrayList<>(hashtags);
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public User getLandlord() {
        return landlord;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public String getAddress() {
        return address;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public Long getDailyFee() {
        return dailyFee;
    }

    public Long getDeposit() {
        return deposit;
    }

    public BigDecimal getArea() {
        return area;
    }

    public List<String> getFacilities() {
        return facilities;
    }

    public List<String> getIndustryRestrictions() {
        return industryRestrictions;
    }

    public List<String> getAdditionalRestrictions() {
        return additionalRestrictions;
    }

    public LocalDate getOperatingStartDate() {
        return operatingStartDate;
    }

    public LocalDate getOperatingEndDate() {
        return operatingEndDate;
    }

    public Integer getMinOperatingDays() {
        return minOperatingDays;
    }

    public Integer getMaxOperatingDays() {
        return maxOperatingDays;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getHashtags() {
        return hashtags;
    }

    public Long getViewCount() {
        return viewCount == null ? 0L : viewCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void update(
            String title,
            List<String> imageUrls,
            String address,
            String detailAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            Long dailyFee,
            Long deposit,
            BigDecimal area,
            List<String> facilities,
            List<String> industryRestrictions,
            List<String> additionalRestrictions,
            LocalDate operatingStartDate,
            LocalDate operatingEndDate,
            Integer minOperatingDays,
            Integer maxOperatingDays,
            String description,
            List<String> hashtags
    ) {
        this.title = title;
        this.imageUrls = new ArrayList<>(imageUrls);
        this.address = address;
        this.detailAddress = detailAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.dailyFee = dailyFee;
        this.deposit = deposit;
        this.area = area;
        this.facilities = new ArrayList<>(facilities);
        this.industryRestrictions = new ArrayList<>(industryRestrictions);
        this.additionalRestrictions = new ArrayList<>(additionalRestrictions);
        this.operatingStartDate = operatingStartDate;
        this.operatingEndDate = operatingEndDate;
        this.minOperatingDays = minOperatingDays;
        this.maxOperatingDays = maxOperatingDays;
        this.description = description;
        this.hashtags = new ArrayList<>(hashtags);
    }

    public void increaseViewCount() {
        if (viewCount == null) {
            viewCount = 1L;
            return;
        }
        viewCount += 1;
    }
}
