CREATE TABLE `place_pet_policies` (
    `external_place_id`    VARCHAR(255)    NOT NULL,
    `allowed_pet_size`    VARCHAR(10)    NULL,
    `leash_required`    BOOLEAN    NULL,
    `carrier_required`    BOOLEAN    NULL,
    `indoor_outdoor_type`    BOOLEAN    NULL,
    `parking`    BOOLEAN    NULL,
    `place_caution`    TEXT    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `breeds` (
    `breed_id`    CHAR(36)    NOT NULL,
    `breed_name`    VARCHAR(30)    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `user_preferences` (
    `user_id`    CHAR(36)    NULL,
    `transport_methods_id`    CHAR(36)    NOT NULL,
    `region_id`    CHAR(36)    NOT NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `places` (
    `external_place_id`    VARCHAR(255)    NOT NULL,
    `theme_id`    CHAR(36)    NOT NULL,
    `place_name`    VARCHAR(100)    NULL,
    `place_image_url`    VARCHAR(500)    NULL,
    `address`    VARCHAR(255)    NULL,
    `latitude`    DECIMAL(10,7)    NULL,
    `longitude`    DECIMAL(10,7)    NULL,
    `business_hours`    VARCHAR(255)    NULL,
    `phone_number`    VARCHAR(30)    NULL,
    `rating`    TINYINT    NULL,
    `review_num`    INT    NULL,
    `visit_num`    INT    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `reviews` (
    `review_id`    CHAR(36)    NOT NULL,
    `place_id`    VARCHAR(255)    NOT NULL,
    `user_id`    CHAR(36)    NULL,
    `pet_id`    CHAR(36)    NOT NULL,
    `rating`    TINYINT    NULL,
    `contents`    TEXT    NULL,
    `recommendation_count`    INT    NULL,
    `created_at`    DATETIME    NULL
);

CREATE TABLE `pet_activities` (
    `activity_id`    CHAR(36)    NOT NULL,
    `activity_name`    VARCHAR(30)    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `user_stamps` (
    `user_stamp_id`    CHAR(36)    NOT NULL,
    `user_id`    CHAR(36)    NULL,
    `stamp_id`    CHAR(36)    NOT NULL,
    `stamp_count`    INT    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `post_recommendations` (
    `post_id`    CHAR(36)    NOT NULL,
    `user_id`    CHAR(36)    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `photos` (
    `photo_id`    CHAR(36)    NOT NULL,
    `user_id`    CHAR(36)    NULL,
    `course_place_id`    CHAR(36)    NOT NULL,
    `photo_url`    VARCHAR(500)    NULL,
    `taken_at`    DATE    NULL,
    `created_at`    DATETIME    NULL
);

CREATE TABLE `post_bookmarks` (
    `user_id`    CHAR(36)    NULL,
    `post_id`    CHAR(36)    NOT NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `start_course` (
    `start_course_id`    INT    NOT NULL,
    `start_course_location`    CHAR(255)    NULL,
    `start_course_time`    DATETIME    NULL
);

CREATE TABLE `course_routes` (
    `route_id`    CHAR(36)    NOT NULL,
    `course_id`    CHAR(36)    NOT NULL,
    `transport_method`    VARCHAR(20)    NULL,
    `total_distance_m`    INT    NULL,
    `total_duration_sec`    INT    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `stamps` (
    `stamp_id`    CHAR(36)    NOT NULL,
    `stamp_name`    VARCHAR(30)    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `themes` (
    `theme_id`    CHAR(36)    NOT NULL,
    `theme_name`    VARCHAR(50)    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `comments` (
    `comment_id`    CHAR(36)    NOT NULL,
    `post_id`    CHAR(36)    NOT NULL,
    `user_id`    CHAR(36)    NULL,
    `post_comment_id`    CHAR(36)    NOT NULL,
    `depth`    INT    NULL,
    `comment_order`    INT    NULL,
    `content`    TEXT    NULL,
    `created_at`    DATETIME    NULL
);

CREATE TABLE `review_recommendations` (
    `review_id`    CHAR(36)    NOT NULL,
    `user_id`    CHAR(36)    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `posts` (
    `post_id`    CHAR(36)    NOT NULL,
    `user_id`    CHAR(36)    NULL,
    `pet_id`    CHAR(36)    NOT NULL,
    `photo_id`    CHAR(36)    NOT NULL,
    `course_id`    CHAR(36)    NOT NULL,
    `title`    VARCHAR(100)    NULL,
    `content`    TEXT    NULL,
    `view_count`    INT    NULL,
    `recommendation_count`    INT    NULL,
    `created_at`    DATETIME    NULL
);

CREATE TABLE `users` (
    `user_id`    CHAR(36)    NULL,
    `google_user_id`    VARCHAR(255)    NULL,
    `email`    VARCHAR(255)    NULL,
    `nickname`    VARCHAR(30)    NULL,
    `role`    VARCHAR(20)    NULL,
    `account_status`    VARCHAR(20)    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `course_weather_records` (
    `weather_id`    CHAR(36)    NOT NULL,
    `course_id`    CHAR(36)    NOT NULL,
    `weather_date`    DATE    NULL,
    `temperature`    TINYINT    NULL,
    `humidity`    TINYINT    NULL,
    `weather_status`    VARCHAR(10)    NULL,
    `weather_caution`    VARCHAR(30)    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `album_photos` (
    `album_photo_id`    CHAR(36)    NOT NULL,
    `album_id`    CHAR(36)    NOT NULL,
    `photo_id`    CHAR(36)    NOT NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `pet_preferences_activities` (
    `pet_id`    CHAR(36)    NOT NULL,
    `activity_id`    CHAR(36)    NOT NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `course_route_legs` (
    `route_leg_id`    CHAR(36)    NOT NULL,
    `route_id`    CHAR(36)    NOT NULL,
    `from_course_place_id`    CHAR(36)    NOT NULL,
    `leg_order`    TINYINT    NULL,
    `distance_m`    INT    NULL,
    `duration_sec`    INT    NULL,
    `leg_polyline`    TEXT    NULL,
    `instruction`    TEXT    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `albums` (
    `album_id`    CHAR(36)    NOT NULL,
    `user_id`    CHAR(36)    NULL,
    `album_name`    VARCHAR(30)    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `keyring_cards` (
    `keyring_card_id`    CHAR(36)    NOT NULL,
    `user_id`    VARCHAR(255)    NULL,
    `pet_id`    CHAR(36)    NOT NULL,
    `photo_id`    CHAR(36)    NOT NULL,
    `course_id`    CHAR(36)    NOT NULL,
    `keyring_card_url`    VARCHAR(500)    NULL,
    `created_at`    DATETIME    NULL
);

CREATE TABLE `regions` (
    `region_id`    CHAR(36)    NOT NULL,
    `region_name`    VARCHAR(100)    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `transport_methods` (
    `transport_method_id`    CHAR(36)    NOT NULL,
    `transport_method_name`    VARCHAR(50)    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `course_places` (
    `course_place_id`    CHAR(36)    NOT NULL,
    `course_id`    CHAR(36)    NOT NULL,
    `external_place_id`    VARCHAR(255)    NOT NULL,
    `visit_order`    TINYINT    NULL,
    `final_place`    BOOLEAN    NULL,
    `is_visited`    BOOLEAN    NULL,
    `visited_at`    DATETIME    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `pets` (
    `pet_id`    CHAR(36)    NOT NULL,
    `user_id`    CHAR(36)    NULL,
    `breed_id`    CHAR(36)    NOT NULL,
    `pet_name`    VARCHAR(50)    NOT NULL,
    `size`    VARCHAR(10)    NULL,
    `age`    TINYINT    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL,
    `Field`    VARCHAR(255)    NULL
);

CREATE TABLE `travel_courses` (
    `course_id`    CHAR(36)    NOT NULL,
    `user_id`    CHAR(36)    NULL,
    `start_course_id`    INT    NOT NULL,
    `travel_date`    DATE    NULL,
    `is_completed`    BOOLEAN    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NOT NULL
);

CREATE TABLE `visit_verifications` (
    `verification_id`    CHAR(36)    NOT NULL,
    `user_id`    CHAR(36)    NULL,
    `course_place_id`    CHAR(36)    NOT NULL,
    `photo_id`    CHAR(36)    NOT NULL,
    `verification_latitude`    DECIMAL(10,7)    NULL,
    `verification_longitude`    DECIMAL(10,7)    NULL,
    `verification_status`    VARCHAR(30)    NULL,
    `verified_at`    DATETIME    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `post_reports` (
    `post_id`    CHAR(36)    NOT NULL,
    `user_id`    CHAR(36)    NULL,
    `report_reason`    VARCHAR(50)    NULL,
    `report_detail`    TEXT    NULL,
    `report_status`    VARCHAR(20)    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);

CREATE TABLE `user_preference_themes` (
    `theme_id`    CHAR(36)    NOT NULL,
    `user_id`    CHAR(36)    NULL,
    `created_at`    DATETIME    NULL,
    `updated_at`    DATETIME    NULL
);
